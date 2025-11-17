# MCIS-JAVA-GUI
GUI For the MCIS Database 
To run the program follow the below steps:
1. Clone the repository locally
2. Go to terminal and ensure you are in the correct folder (ls to check, there should be the dbmcis.java file and the jar file)
3. Open the dbmcis.java file and edit the following lines:
4. a.Change "USER" to your oracle username "private static final String USER = USER"
5. b.Change "PASS" to your oracle password "private static final String USER = PASS"
6. Run the following lines of code in your terminal:
7. javac -cp ".;ojdbc17.jar" dbmcis.java
8. java -cp ".;ojdbc17.jar" dbmcis
