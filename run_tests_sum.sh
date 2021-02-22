for N in 10 100 1000 10000 100000 1000000 10000000 100000000
do
	./nvidia_docker_run.sh tornado ReductionAvgFloats $N >> without_gpu.txt
done
