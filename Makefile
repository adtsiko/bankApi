build-app:
	sbt docker:publishLocal

format:
	sbt scalafmtAll 
