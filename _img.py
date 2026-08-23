import struct
with open('ledger.png','rb') as f:
    data = f.read()
w,h = struct.unpack('>II', data[16:24])
print('size', w, h, 'bytes', len(data))
