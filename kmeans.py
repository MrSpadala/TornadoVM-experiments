import numpy as np
from pdb import set_trace
import time

n = 170000
d = 100
k = 9
print(n,d,k)

n_iters = 10

X = np.random.random((n,d))
C = np.random.random((k,d))

start = time.time()
for i in range(n_iters):
	c_mask = ((X - C[:, np.newaxis, :])**2).sum(axis=2).argmin(axis=0)
	C = np.array([X[c_mask==k].mean(axis=0) for k in range(C.shape[0])])
end = time.time()

print("elapsed:", end-start)