import configparser
import os
from pathlib import Path

cfg = configparser.ConfigParser()
cfg.read(Path(os.path.dirname(__file__)) / ".." / "cerberbot.ini")
