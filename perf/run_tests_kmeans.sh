for N in 200 2000 20000 200000 2000000
do
	./nvidia_docker_run.sh tornado KMeans $N >> kmeans.txt
done
