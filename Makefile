PATH_PROJECT_JAR = target/p2p_recommendation-0.0.1-SNAPSHOT.jar
PROJECT_GROUP    = p2p_recommendation
JADE_AGENTS      = SeederAgent1:$(PROJECT_GROUP).SeederAgent; ClientAgent1:$(PROJECT_GROUP).ClientAgent("1984")
JADE_FLAGS 		 = -gui -agents "$(JADE_AGENTS)"

.PHONY:
	clean
	build-and-run

build-and-run:
	@echo "Gerando a build e executando o projeto"
	make build run

build:
	@echo "Gerando a build do projeto"
	mvn clean install

run:
	@echo "Executando o projeto com a última build criada"
	java -cp $(PATH_PROJECT_JAR) jade.Boot $(JADE_FLAGS)

clean:
	@echo "Removendo a build do projeto"
	mvn clean
	rm -f APDescription.txt; rm -f MTPs-Main-Container.txt