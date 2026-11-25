"""Drives the S9 spike on a running emulator with adb + uiautomator and prints PASS/FAIL per check."""

import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

ADB = os.path.join(os.environ["ANDROID_HOME"], "platform-tools", "adb")
PKG = "com.fernandocorreia.khepri.spike.nav3"
results = []


def adb(*args):
    return subprocess.run([ADB, *args], capture_output=True, text=True, timeout=60).stdout


def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("shell", "cat", "/sdcard/ui.xml")
    try:
        return list(ET.fromstring(xml).iter("node"))
    except ET.ParseError:
        return []


def texts():
    return [n.get("text") for n in nodes() if n.get("text")]


def tap_text(text):
    for n in nodes():
        if n.get("text") == text:
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
            adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
            time.sleep(1.2)
            return True
    raise RuntimeError(f"no node with text {text!r}; on screen: {texts()}")


def tab(name):
    # Tab labels are the bottom-most node with that exact text.
    candidates = [n for n in nodes() if n.get("text") == name]
    if not candidates:
        raise RuntimeError(f"tab {name} not found; on screen: {texts()}")
    n = max(candidates, key=lambda n: int(re.findall(r"\d+", n.get("bounds"))[1]))
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(1.2)


def back():
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    time.sleep(1.2)


def type_note(value):
    for n in nodes():
        if n.get("class") == "android.widget.EditText":
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
            adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
            time.sleep(0.5)
            adb("shell", "input", "text", value)
            adb("shell", "input", "keyevent", "KEYCODE_ESCAPE")
            time.sleep(0.8)
            return
    raise RuntimeError("no text field")


def check(name, cond, detail=""):
    results.append((name, bool(cond)))
    print(("PASS " if cond else "FAIL ") + name + ("" if cond else f"  [{detail}]"), flush=True)


def title():
    t = texts()
    for s in t:
        if " root" in s or "detail" in s or s.startswith(("Goal", "Settings", "Workout", "Thread")):
            return s
    return str(t)


def vm_stamp():
    return next((t for t in texts() if t.startswith("vm#")), None)


def on_screen(s):
    return s in texts()


def launch(data=None):
    if data:
        adb("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", data, PKG)
    else:
        adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    time.sleep(2.5)


def fresh():
    adb("shell", "am", "force-stop", PKG)
    # Remove the old task too, or the next start replays its base intent.
    adb("shell", "am", "start", "-W", "--activity-clear-task", "-f", "0x10000000", "-n", f"{PKG}/.MainActivity")
    adb("shell", "am", "force-stop", PKG)
    time.sleep(1)


fresh()
launch()
check("launch shows Today root", on_screen("Today root"), title())

# 1. Five tabs, each with its own stack.
check("five tabs", all(on_screen(t) for t in ["Today", "Coach", "Training", "Progress", "More"]), texts())
tab("More")
type_note("kept")
tap_text("Goals")
tap_text("Goal g1")
check("More stack: Goal g1", on_screen("Goal g1"), title())
goal_vm = vm_stamp()
tab("Coach")
tap_text("Open detail")
check("Coach stack: detail 1", on_screen("Coach detail 1"), title())
tab("More")
check("More stack kept across tab switch", on_screen("Goal g1"), title())
check("Goal g1 ViewModel survives a tab switch", vm_stamp() == goal_vm, f"{goal_vm} -> {vm_stamp()}")
back()
check("back pops within the tab", on_screen("Goals"), title())
back()
check("More root entry state kept (saveable note)", on_screen("kept"), texts())

# 2. Re-select pops to root.
tap_text("Goals")
tap_text("Goal g1")
check("popped Goal g1 gets a new ViewModel when reopened", vm_stamp() not in (None, goal_vm), f"{goal_vm} -> {vm_stamp()}")
tab("More")
check("re-select pops to root", on_screen("More root"), title())

# 3. Sheet entry on a tab's stack; back dismisses it.
tap_text("Settings sheet")
check("sheet shows over More root", on_screen("Settings (sheet)"), texts())
back()
check("back dismisses the sheet", on_screen("More root") and not on_screen("Settings (sheet)"), texts())

# 4. Full-screen entry hides the tab bar.
tab("Training")
tap_text("Start workout")
check("full-screen workout hides tab bar", on_screen("Workout (full screen)") and not on_screen("Progress"), texts())
back()
check("tab bar back after leaving workout", on_screen("Training root") and on_screen("Progress"), texts())

# 5. Back: tab root returns to Today, Today root exits.
tab("Coach")
check("Coach still on detail 1", on_screen("Coach detail 1"), title())
back()
back()
check("back from a tab root returns to Today", on_screen("Today root"), title())

# 5b. Rotation keeps the tab, the stack and the entry's ViewModel.
tab("More")
tap_text("Goals")
before = vm_stamp()
adb("shell", "settings", "put", "system", "accelerometer_rotation", "0")
adb("shell", "settings", "put", "system", "user_rotation", "1")
time.sleep(3)
check("rotation keeps More stack and ViewModel", on_screen("Goals") and vm_stamp() == before, f"{title()} {before} -> {vm_stamp()}")
adb("shell", "settings", "put", "system", "user_rotation", "0")
time.sleep(3)
back()
tab("Today")

# 6. Process death restores selected tab and every stack.
tab("More")
tap_text("Goals")
tap_text("Goal g1")
tab("Coach")
tap_text("Open detail")
adb("shell", "input", "keyevent", "KEYCODE_HOME")
time.sleep(1.5)
pid = adb("shell", "pidof", PKG).strip()
for _ in range(20):
    adb("shell", "am", "kill", PKG)
    time.sleep(1)
    pid = adb("shell", "pidof", PKG).strip()
    if not pid:
        break
check("process was killed", pid == "", pid)
launch()
check("after process death: Coach tab and its stack restored", on_screen("Coach detail 1"), title())
tab("More")
check("after process death: More stack restored", on_screen("Goal g1"), title())
back()
back()
check("after process death: More root note restored", on_screen("kept"), texts())

# 7. Back from Today root exits.
tab("Today")
back()
time.sleep(1)
focus = adb("shell", "dumpsys", "activity", "activities")
resumed = re.search(r"mResumedActivity.*|topResumedActivity.*", focus)
check("back at Today root leaves the app", resumed is None or PKG not in resumed.group(0), resumed.group(0) if resumed else "")

# 8. Deep link from outside builds parents (cold start).
fresh()
launch("khepri://goals/g42")
check("cold link: Goal g42 on More", on_screen("Goal g42"), title())
back()
check("cold link: parent Goals in place", on_screen("Goals"), title())
back()
check("cold link: More root under it", on_screen("More root"), title())
back()
check("cold link: back from More root returns to Today", on_screen("Today root"), title())

# 9. Link while running (onNewIntent) lands, and the same link twice still lands.
tab("Training")
launch("khepri://coach/t9")
check("warm link: Coach thread t9, tab bar hidden", on_screen("Thread t9 (no tab bar)") and not on_screen("Progress"), texts())
back()
launch("khepri://coach/t9")
check("same link twice still lands", on_screen("Thread t9 (no tab bar)"), texts())

failed = [n for n, ok in results if not ok]
print(f"\n{len(results) - len(failed)}/{len(results)} passed")
sys.exit(1 if failed else 0)
