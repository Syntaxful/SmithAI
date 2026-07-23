.PHONY: build test release clean docker

build:
	./build.sh

test:
	cd SmithAI && mvn clean test
	python -m py_compile SmithAI-Server/app.py
	python SmithAI-Server/test_app.py

release:
	./package-release.sh

clean:
	cd SmithAI && mvn clean
	rm -rf release

docker:
	docker-compose build

docker-up:
	docker-compose up
