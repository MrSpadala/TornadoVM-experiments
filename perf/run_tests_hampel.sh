for N in 100 1000 10000 100000 1000000 10000000
do
	./nvidia_docker_run.sh tornado WindowOperations $N >> hampel.txt
done
