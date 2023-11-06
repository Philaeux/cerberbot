# CerberBOT
Bot for the AdmiralBulldog community.

Technologies used:
- Python 3

## For Developers

A secret file is not pushed to the repository. You need to create it at `src/cerberbot.ini`. Use the reference `src/cerberbot.example.ini`.

### Windows with Powershell
Make sure you have Python Windows installed.

Setup the environment:
```
cd src
python -m venv .env
.env\Scripts\python.exe -m pip install --upgrade pip
.env\Scripts\python.exe -m pip install -r .\requirements.txt
"$(get-location)" > .env\Lib\site-packages\cerberbot.pth
```
Then you can run the bot with
```
.\.env\Scripts\python.exe main.py
```

### Unix

Setup the environment
```
cd src
python3 -m venv .venv
.venv/bin/pip3 install --upgrade pip
.venv/bin/pip3 install -r requirements.txt
$(foreach dir, $(wildcard .venv/lib/*), echo $(shell pwd) > $(dir)/site-packages/cerberbot.pth &&) echo
```
Then you can run the bot with
```
.venv/bin/python3 ./main.py
```
