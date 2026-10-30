import gzip, io, json, struct, sys

TAG_END=0;TAG_BYTE=1;TAG_SHORT=2;TAG_INT=3;TAG_LONG=4;TAG_FLOAT=5;TAG_DOUBLE=6
TAG_BYTE_ARRAY=7;TAG_STRING=8;TAG_LIST=9;TAG_COMPOUND=10;TAG_INT_ARRAY=11;TAG_LONG_ARRAY=12

def _read(fmt, f):
    return struct.unpack(fmt, f.read(struct.calcsize(fmt)))[0]

class NBT:
    @staticmethod
    def load(path):
        with gzip.open(path,'rb') as f:
            t=f.read(1)[0]
            assert t==TAG_COMPOUND, t
            name=NBT._str(f)
            return NBT._payload(f, t)
    @staticmethod
    def _str(f):
        n=_read('>H',f); return f.read(n).decode('utf-8')
    @staticmethod
    def _payload(f,t):
        if t==TAG_BYTE: return _read('>b',f)
        if t==TAG_SHORT: return _read('>h',f)
        if t==TAG_INT: return _read('>i',f)
        if t==TAG_LONG: return _read('>q',f)
        if t==TAG_FLOAT: return _read('>f',f)
        if t==TAG_DOUBLE: return _read('>d',f)
        if t==TAG_BYTE_ARRAY:
            n=_read('>i',f); return list(f.read(n))
        if t==TAG_STRING: return NBT._str(f)
        if t==TAG_LIST:
            et=f.read(1)[0]; n=_read('>i',f)
            return [NBT._payload(f,et) for _ in range(n)]
        if t==TAG_COMPOUND:
            d={}
            while True:
                tt=f.read(1)[0]
                if tt==TAG_END: break
                nm=NBT._str(f)
                d[nm]=NBT._payload(f,tt)
            return d
        if t==TAG_INT_ARRAY:
            n=_read('>i',f); return [_read('>i',f) for _ in range(n)]
        if t==TAG_LONG_ARRAY:
            n=_read('>i',f); return [_read('>q',f) for _ in range(n)]
        raise ValueError(t)
    @staticmethod
    def save(root,path):
        buf=io.BytesIO()
        buf.write(bytes([TAG_COMPOUND]))
        NBT._write_str(buf,'')
        NBT._write_payload(buf,root,TAG_COMPOUND)
        with open(path,'wb') as raw:
            with gzip.GzipFile(fileobj=raw, mode='wb', mtime=0) as f:
                f.write(buf.getvalue())
    @staticmethod
    def _write_str(f,s):
        b=s.encode('utf-8'); f.write(struct.pack('>H',len(b))); f.write(b)
    @staticmethod
    def _write_payload(f,v,t):
        if t==TAG_BYTE: f.write(struct.pack('>b',v))
        elif t==TAG_SHORT: f.write(struct.pack('>h',v))
        elif t==TAG_INT: f.write(struct.pack('>i',v))
        elif t==TAG_LONG: f.write(struct.pack('>q',v))
        elif t==TAG_FLOAT: f.write(struct.pack('>f',v))
        elif t==TAG_DOUBLE: f.write(struct.pack('>d',v))
        elif t==TAG_BYTE_ARRAY:
            f.write(struct.pack('>i',len(v))); f.write(bytes(v))
        elif t==TAG_STRING: NBT._write_str(f,v)
        elif t==TAG_LIST:
            et,items=v
            f.write(bytes([et])); f.write(struct.pack('>i',len(items)))
            for it in items: NBT._write_payload(f,it,et)
        elif t==TAG_COMPOUND:
            for k,val in v.items():
                tt,payload=val if isinstance(val,tuple) else (NBT._infer(val),val)
                f.write(bytes([tt])); NBT._write_str(f,k); NBT._write_payload(f,payload,tt)
            f.write(bytes([TAG_END]))
        elif t==TAG_INT_ARRAY:
            f.write(struct.pack('>i',len(v)))
            for it in v: f.write(struct.pack('>i',it))
        elif t==TAG_LONG_ARRAY:
            f.write(struct.pack('>i',len(v)))
            for it in v: f.write(struct.pack('>q',it))
    @staticmethod
    def _infer(v):
        if isinstance(v,bool): return TAG_BYTE
        if isinstance(v,int): return TAG_INT
        if isinstance(v,float): return TAG_DOUBLE
        if isinstance(v,str): return TAG_STRING
        if isinstance(v,list):
            if not v: return (TAG_LIST,(TAG_END,[]))
            raise ValueError('ambiguous empty list; pass tuple(type, items)')
        if isinstance(v,dict): return TAG_COMPOUND
        raise ValueError(type(v))

def show(root, maxdepth=4):
    def conv(v,depth):
        if depth>maxdepth: return '...'
        if isinstance(v,dict): return {k:conv(x,depth+1) for k,x in v.items()}
        if isinstance(v,list):
            if len(v)>8: return [conv(x,depth+1) for x in v[:4]]+['...%d'%len(v)]
            return [conv(x,depth+1) for x in v]
        return v
    return conv(root,0)

if __name__=='__main__':
    path=sys.argv[1]
    root=NBT.load(path)
    print(json.dumps(show(root),ensure_ascii=False,indent=1,default=str))
