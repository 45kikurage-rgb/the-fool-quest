# Compatibility entry point for the existing workflow; pins live in the versioned test.
import runpy
runpy.run_path("release-tests/foolquest-code18-upgrade.py", run_name="__main__")
