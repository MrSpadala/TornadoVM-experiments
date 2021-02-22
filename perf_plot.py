import matplotlib.pyplot as plt
import numpy as np
import time
import sys

from matplotlib import rcParams

rcParams['mathtext.fontset'] = 'stix'
rcParams['font.family'] = 'STIXGeneral'
rcParams['font.size'] = 13
rcParams['legend.fontsize'] = "small"


def plot_sum():
	#
	# REDUCTION SUM
	#

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
	plt.plot(N, with_gpu, label="tornado", linestyle="--", marker="^", linewidth=0.9)
	plt.plot(N, without_gpu, label="sequential", linestyle="--", marker="^", linewidth=0.9)
	plt.plot(N, with_numpy, label="numpy", linestyle="--", marker="^", linewidth=0.9)
	plt.legend()
	plt.grid(True)
	plt.ylabel("Time, ms")
	plt.xlabel("Input array size")
	plt.gca().set_xscale('log')
	plt.savefig("sum_linear.png", dpi=200)
	plt.gca().set_yscale('log')
	plt.savefig("sum_log.png", dpi=200)
	plt.show()


def plot_hampel():
	#
	# HAMPEL FILTER
	#

	N = [100, 1000, 10000, 100000, 1000000, 10000000]

	without_gpu = np.array([
		307027,
		2146276,
		4758516,
		34458110,
		360644686,
		3422543550,
	]) * 10**-6

	with_gpu = np.array([
		126628,
		146799,
		167266,
		718400,
		6876979,
		55623084,
	]) * 10**-6

	start_index = 0
	N = N[start_index:]
	with_gpu = with_gpu[start_index:]
	without_gpu = without_gpu[start_index:]

	plt.title("Hampel")
	plt.plot(N, with_gpu, label="tornado", linestyle="--", marker="^", linewidth=0.9)
	plt.plot(N, without_gpu, label="sequential", linestyle="--", marker="^", linewidth=0.9)
	plt.legend()
	plt.grid(True)
	plt.ylabel("Time, ms")
	plt.xlabel("Input array size")
	plt.gca().set_xscale('log')
	plt.savefig("hampel_linear.png", dpi=200)
	plt.gca().set_yscale('log')
	plt.savefig("hampel_log.png", dpi=200)
	plt.show()


def plot_kmeans():
	#
	# KMEANS
	#

	N = np.array([200, 2000, 20000, 200000, 2000000]) * 50 * 5  #varies values of n, fixed d, k

	without_gpu = np.array([
		4920716,
		15257807,
		58564819,
		468063397,
		4604026156,
	]) * 10**-6

	with_gpu = np.array([
		11777419,
		13787613,
		21859670,
		96262029,
		831750753,
	]) * 10**-6

	start_index = 0
	N = N[start_index:]
	with_gpu = with_gpu[start_index:]
	without_gpu = without_gpu[start_index:]

	plt.title("k-means")
	plt.plot(N, with_gpu, label="tornado", linestyle="--", marker="^", linewidth=0.9)
	plt.plot(N, without_gpu, label="sequential", linestyle="--", marker="^", linewidth=0.9)
	plt.legend()
	plt.grid(True)
	plt.ylabel("Time, ms")
	plt.xlabel("Input size, N*K*D")
	plt.gca().set_xscale('log')
	plt.savefig("kmeans_linear.png", dpi=200)
	plt.gca().set_yscale('log')
	plt.savefig("kmeans_log.png", dpi=200)
	plt.show()


if __name__ == '__main__':
	if len(sys.argv) < 2:
		raise Exception("need an argument")
	if sys.argv[1] == "sum":
		plot_sum()
	elif sys.argv[1] == "hampel":
		plot_hampel()
	elif sys.argv[1] == "kmeans":
		plot_kmeans()
	else:
		raise ValueError(f"invalid arg {sys.argv[1]}")
