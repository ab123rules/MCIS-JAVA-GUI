# MCIS-JAVA-GUI
GUI For the MCIS Database 
To run the program follow the below steps:
1. Clone the repository locally
2. Go to terminal and ensure you are in the correct folder (ls to check, there should be the dbmcis.java file and the jar file)
3. Open the dbmcis.java file and edit the following lines:
   a.Change "USER" to your oracle username "private static final String USER = USER"
   b.Change "PASS" to your oracle password "private static final String USER = PASS"
4. Run the following lines of code in your terminal:
   a. javac -cp ".;ojdbc17.jar" dbmcis.java
   b. java -cp ".;ojdbc17.jar" dbmcis
