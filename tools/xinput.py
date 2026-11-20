import ctypes as C,sys,time,os,subprocess,json,datetime
X=C.CDLL('libX11.so.6');T=C.CDLL('libXtst.so.6')
X.XOpenDisplay.argtypes=[C.c_char_p];X.XOpenDisplay.restype=C.c_void_p;d=X.XOpenDisplay(b':99')
if not d:raise SystemExit('X display unavailable')
X.XStringToKeysym.argtypes=[C.c_char_p];X.XStringToKeysym.restype=C.c_ulong
X.XKeysymToKeycode.argtypes=[C.c_void_p,C.c_ulong];X.XKeysymToKeycode.restype=C.c_uint
X.XFlush.argtypes=[C.c_void_p];X.XSync.argtypes=[C.c_void_p,C.c_int]
T.XTestFakeKeyEvent.argtypes=[C.c_void_p,C.c_uint,C.c_int,C.c_ulong]
T.XTestFakeButtonEvent.argtypes=[C.c_void_p,C.c_uint,C.c_int,C.c_ulong]
T.XTestFakeMotionEvent.argtypes=[C.c_void_p,C.c_int,C.c_int,C.c_int,C.c_ulong]
X.XSetInputFocus.argtypes=[C.c_void_p,C.c_ulong,C.c_int,C.c_ulong]
P={' ':'space','/':'slash',':':'colon','_':'underscore','-':'minus','=':'equal','.':'period',',':'comma','[':'bracketleft',']':'bracketright','+':'plus',';':'semicolon','?':'question'}
def keycode(key):
 k=X.XKeysymToKeycode(d,X.XStringToKeysym(key.encode()))
 if not k:raise ValueError('Unknown key '+key)
 return k
def event(key,down):T.XTestFakeKeyEvent(d,keycode(key),int(down),0);X.XFlush(d)
def press(key):event(key,True);event(key,False);time.sleep(.05)
def text(value):
 for ch in value:
  shifted=ch.isupper() or ch in ':_+?'
  if shifted:event('Shift_L',True)
  event(P.get(ch,ch.lower()),True);event(P.get(ch,ch.lower()),False)
  if shifted:event('Shift_L',False)
  time.sleep(.012)
 X.XSync(d,False)
with open('/workspace/ZorykaiMenus/evidence/actions.jsonl','a') as log:log.write(json.dumps({'at_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'action':sys.argv[1:]})+'\n')
cmd=sys.argv[1]
if cmd=='focus':X.XSetInputFocus(d,int(sys.argv[2],0),1,0);X.XFlush(d)
elif cmd=='chord':
 event(sys.argv[2],True);press(sys.argv[3]);event(sys.argv[2],False)
elif cmd=='key':
 for k in sys.argv[2:]:press(k)
elif cmd=='text':text(sys.argv[2])
elif cmd in ['click','rightclick']:
 x,y=map(int,sys.argv[2:4]);T.XTestFakeMotionEvent(d,-1,x,y,0);X.XFlush(d);time.sleep(.15);T.XTestFakeButtonEvent(d,3 if cmd=='rightclick' else 1,1,0);T.XTestFakeButtonEvent(d,3 if cmd=='rightclick' else 1,0,0);X.XFlush(d)
elif cmd=='move':T.XTestFakeMotionEvent(d,-1,int(sys.argv[2]),int(sys.argv[3]),0);X.XFlush(d)
elif cmd=='command':press('t');time.sleep(.25);text(sys.argv[2]);press('Return')
else:raise ValueError(cmd)

time.sleep(.5)
