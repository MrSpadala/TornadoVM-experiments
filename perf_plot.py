import matplotlib.pyplot as plt
import numpy as np
import time

N = [10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000]

without_gpu = np.array([
	240,
	1592,
	15735,
	52561,
	101501,
	1033940,
	10752692,
	107470408,
]) * 10**-6

with_gpu = np.array([
	284994,
	299743,
	302003,
	683136,
	380664,
	1435724,
	9539856,
	87032585,
]) * 10**-6

# Numpy test
with_numpy = []
for i in N:
	X = np.random.random(i)
	start = time.time()
	s = np.sum(X)
	end = time.time()
	with_numpy.append((end - start) * 10**3)
with_numpy = np.array(with_numpy)

start_index = 2
N = N[start_index:]
with_gpu = with_gpu[start_index:]
without_gpu = without_gpu[start_index:]
with_numpy = with_numpy[start_index:]

plt.title("Sum")
plt.plot(N, with_gpu, label="tornado")
plt.plot(N, without_gpu, label="sequential")
plt.plot(N, with_numpy, label="numpy")
plt.legend()
plt.ylabel("Time, ms")
plt.xlabel("Input array size")
plt.gca().set_xscale('log')
plt.gca().set_yscale('log')
plt.show()


