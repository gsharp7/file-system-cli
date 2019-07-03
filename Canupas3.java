import java.util.*;
import java.io.*;
import java.util.regex.*;

class Canupas3 implements Serializable{

     static final int FILE = 0, DIRECTORY = 1;
     static final String initFileContent = "#include <iostream>\n\nusing namespace"
                                           + " std;\n\nint main(){\n\tcout<<\"Hello World!\\n\"<<endl;\n\t"
                                           + "return 0;\n}";
     static final String appendFileContent = "\n//this is a test for the append using >>";
     static final String editFileContent = "\n//this is the result of editing the file using edit";
     Tree tree;
     static FileWriter outFileWriter; //file writer for writing program output to "mp3.out" file
     String fileTextBuffer = "";
     transient Scanner inputFile;

     public static void main(String args[]){
          Canupas3 cmd = new Canupas3();
          System.out.println("#################################");
          System.out.println("#      Virtual File System      #");
          System.out.println("#################################");
          System.out.println("\nTo make a recovery file, type the command 'save'.\n");
          try{
               cmd.readTree();
               while(cmd.inputFile.hasNextLine()){
                    cmd.processInput(cmd.inputFile.nextLine());
               }
               cmd.writeOutputToFile();
               Scanner sc = new Scanner(System.in);
               while(true){
                    System.out.print(cmd.tree.currentPath() + ">");
                    cmd.processInput(sc.nextLine());
               }
          }
          catch(Exception e){ //this is to simulate the Ctrl+C key combo to exit a console application
               System.out.println("\nSaving file tree and exiting program...");
               return;
          }
     }

     Canupas3(){
          tree = new Tree(this);
          initializeFiles();
     }

     void initializeFiles(){ //prepare the input, output, and save files
          try{
               inputFile = new Scanner(new File("mp3.in").getAbsoluteFile());
               outFileWriter = new FileWriter(new File("mp3.out").getAbsoluteFile());
               outFileWriter.write("");
          }
          catch(IOException e){
               System.out.println("An error occured trying to prepare the files.");
               e.printStackTrace();
          }
          catch(Exception e){
               e.printStackTrace();
          }
     }

     void writeOutputToFile(){
          try{
               outFileWriter.write(fileTextBuffer);
               fileTextBuffer = ""; //empty the file text buffer to avoid memory hogging
               outFileWriter.close();
          }
          catch(IOException ioe){
               System.out.println("An error occured while trying to write the output to mp3.out.");
               ioe.printStackTrace();
          }
          catch(Exception e){
               System.out.println("An error occured.");
               e.printStackTrace();
          }
     }

     public void saveTree(){
          System.out.println("Saving file tree to file: tree.ser");
          try{
               ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File("tree.ser")));
               oos.writeObject(tree);
               oos.close();
          }
          catch(IOException e){
               System.out.println("An error occured while trying to create the tree.ser file." + "\n");
               e.printStackTrace();
          }
          catch(Exception e){
               System.out.println("An error occured while trying to create the tree.ser file.");
               e.printStackTrace();
          }
     }

     public void readTree(){
          System.out.println("Trying to find a recory file...");
          try {
              FileInputStream fileIn = new FileInputStream("tree.ser");
              ObjectInputStream in = new ObjectInputStream(fileIn);
              tree = (Tree) in.readObject();
              in.close();
              fileIn.close();
              System.out.println("Recovered file tree from tree.ser...");
         }catch(IOException e) {
               //e.printStackTrace();
               return;
          }catch(ClassNotFoundException c) {
               System.out.println("Error deserializing tree: Tree class not found");
               c.printStackTrace();
               return;
          }

     }

     public void processInput(String command){
          CommandRegexList cmdrgxlist = new CommandRegexList();
          Pattern pattern;
          Matcher matcher;
          CommandRegexList c = new CommandRegexList();
          for(int i = 0; i < c.cmdrgx.length; i++){
               pattern = Pattern.compile(c.cmdrgx[i].regex);
               matcher = pattern.matcher(command);
               if(matcher.find()){
                    executeCommand(command, c.cmdrgx[i].meaning);
                    return;
               }
          }
          output("Invalid or unknown command.");
     }

     public void executeCommand(String rawinput, String command){
          switch(command){
               case "print tree":{
                    tree.print();
                    break;
               }
               case "save tree":{
                    saveTree();
                    break;
               }
               case "list contents":{
                    if(rawinput.equals("ls")) //ls with no regex
                         tree.listSubdirs(tree.currentDirectory);
                    else{
                         String regex = parsePath("ls", rawinput);
                         String rgx = regex.replace("*", "\\w*");
                         rgx = rgx.replace(".", "\\.");
                         for(int i = 0; i < tree.currentDirectory.children.size(); i++){
                              if(tree.currentDirectory.children.get(i).fileDesc.fileName.matches(rgx))
                                   output(tree.currentDirectory.children.get(i).fileDesc.fileName);
                         }
                    }
                    break;
               }
               case "list contents absolute":{
                    Node dir = pathToNode(parsePath("ls", rawinput), false);
                    if(dir != null)
                         tree.listSubdirs(dir);
                    else
                         output("ls: " + parsePath("ls", rawinput) + ": No such directory");
                    break;
               }
               case "make directory":{
                    String p = parsePath("mkdir", rawinput);
                    Node dir = pathToNode(p, false);
                    if(dir != null && dir.fileDesc.type == DIRECTORY){
                         //target directory already exists
                         output("mkdir: " + p + ": Already exists.");
                    }
                    else{
                         Node targetDirectory = pathToNode(p, true);
                         String parr[] = p.split("/");
                         if(targetDirectory == null)
                              output("mkdir: " + p + ": Invalid target directory.");
                         else
                              targetDirectory.children.add(new Node(new FileDescriptor(parr[parr.length - 1], DIRECTORY), targetDirectory));
                    }
                    break;
               }
               case "change directory":{
                    String p = parsePath("cd", rawinput);
                    Node target = pathToNode(p, false);
                    String parr[] = p.split("/");
                    boolean dirFound = false;
                    if(target == null || target.fileDesc.type != DIRECTORY)
                         output("cd: " + p + ": No such directory.");
                    else{
                         tree.currentDirectory = target;
                         dirFound = true;
                    }
                    break;
               }
               case "remove directory":{
                    String p = parsePath("rmdir", rawinput);
                    Node target = pathToNode(p, false);
                    if(target == null || target.fileDesc.type != DIRECTORY)
                         output("rmdir: " + p + " No such directory.");
                    else if(target == tree.root)
                         output("Cannot remove root directory!");
                    else{
                         Node pr = target.parent;
                         for(int i = 0; i < pr.children.size(); i++){
                              if(pr.children.get(i).equals(target)){
                                   pr.children.remove(i);
                                   break;
                              }
                         }
                    }
                    break;
               }
               case "rename file/directory":{
                    String input = parsePath("rn", rawinput);
                    String inputarr[] = input.split(" ");
                    Node target = pathToNode(inputarr[0], false);
                    Node renamedParent = pathToNode(inputarr[1], true);
                    if(target == null)
                         output("rn: " + inputarr[0] + " No such directory.");
                    else{
                         if(!target.parent.equals(renamedParent)){
                              output("Invalid rename command. Must be the same directory.");
                         }
                         else{
                              String s[] = inputarr[1].split("/");
                              target.fileDesc.fileName = s[s.length - 1];
                         }
                    }
                    break;
               }
               case "copy file/directory":{
                    String input = parsePath("cp", rawinput);
                    String inputarr[] = input.split(" "); //parse the source-destination paths
                    copy(inputarr[0], inputarr[1], "cp");
                    break;
               }
               case "move file/directory":{
                    String input = parsePath("mv", rawinput);
                    String inputarr[] = input.split(" "); //parse the source-destination paths
                    //copy the source file
                    copy(inputarr[0], inputarr[1], "mv");
                    Node source = pathToNode(inputarr[0], false);
                    if(source != null){
                         //remove the source file
                         for(int i = 0; i < source.parent.children.size(); i++){
                              if(source.parent.children.get(i).equals(source)){
                                   source.parent.children.remove(i);
                                   break;
                              }
                         }
                    }
                    break;
               }
               case "create a file":{
                    String p = parsePath(">", rawinput);
                    Node testTarget = pathToNode(p, false);
                    if(testTarget != null && testTarget.fileDesc.type == FILE){
                         //file already exists, overwrite the file
                         testTarget.fileDesc.content = initFileContent;
                    }
                    else{
                         //file does not exist, create a new one
                         String parr[] = p.split("/");
                         Node targetDirectory = pathToNode(p, true);
                         Node file = new Node(new FileDescriptor(parr[parr.length - 1], FILE), targetDirectory);
                         file.fileDesc.content = initFileContent;
                         targetDirectory.children.add(file);
                    }
                    break;
               }
               case "append to file":{
                    String p = parsePath(">>", rawinput);
                    Node testTarget = pathToNode(p, false);
                    if(testTarget != null && testTarget.fileDesc.type == FILE){
                         //file already exists, overwrite the file
                         testTarget.fileDesc.content += appendFileContent;
                    }
                    else //file does not exist, show error message
                         output(">>: " + p + ": No such file.");
                    break;
               }
               case "edit file":{
                    String p = parsePath("edit", rawinput);
                    Node testTarget = pathToNode(p, false);
                    if(testTarget != null && testTarget.fileDesc.type == FILE){
                         //file already exists, overwrite the file
                         testTarget.fileDesc.content += editFileContent;
                    }
                    else //file does not exist, show error message
                         output("edit: " + p + ": No such file.");
                    break;
               }
               case "remove file":{
                    String p = parsePath("rm", rawinput);
                    Node testTarget = pathToNode(p, false);
                    if(testTarget != null && testTarget.fileDesc.type == FILE){
                         //file already exists, remove the file
                         for(int i = 0; i < testTarget.parent.children.size(); i++){
                              if(testTarget.parent.children.get(i).equals(testTarget)){
                                   testTarget.parent.children.remove(i);
                                   break;
                              }
                         }
                    }
                    else //file does not exist, show error message
                         output("rm: " + p + ": No such file.");
                    break;
               }
               case "show file":{
                    String p = parsePath("show", rawinput);
                    Node file = pathToNode(p, false);
                    if(file != null && file.fileDesc.type == FILE) //file exists so show content
                         output(file.fileDesc.content);
                    else //file does not exists, show error message
                         output("show: " + p + ": No such file.");

                    break;
               }
               case "search file/directory":{
                    String name = parsePath("whereis", rawinput);
                    if(!tree.searchNode(name)){
                         output("whereis: " + name + ": No matches found.");
                    }
                    break;
               }
               //usage cases
               case "mkdir usage":{
                    output("usage: mkdir <directory name>");
                    break;
               }
               case "cd usage":{
                    output("usage: cd <directory name>");
                    break;
               }
               case "rmdir usage":{
                    output("usage: rmdir <directory name>");
                    break;
               }
               case "cp usage":{
                    output("usage: cp source_file/source_directory target_file/target_directory");
                    break;
               }
               case "mv usage":{
                    output("usage: mv source_file/source_directory target_file/target_directory");
                    break;
               }
               case "rm usage":{
                    output("usage: rm <filename>");
                    break;
               }
               case "> usage":{
                    output("usage: > <filename>");
                    break;
               }
               case ">> usage":{
                    output("usage: >> <filename>");
                    break;
               }
               case "show usage":{
                    output("usage: show <filename>");
                    break;
               }
               case "whereis usage":{
                    output("usage: whereis <directory/file name>");
                    break;
               }
               case "invalid":{
                    break;
               }
          }
     }

     public String parsePath(String cmdToken, String rawinput){
          rawinput = rawinput.substring(cmdToken.length(), rawinput.length());
          while(rawinput.charAt(0) == ' '){
               rawinput = rawinput.substring(1, rawinput.length());
          }
          return rawinput;
     }

     public void copy(String sourcePath, String targetPath, String operation){
          Node source = pathToNode(sourcePath, false);
          if(source != null){
               //Make a copy of the source file/directory
               Node sourceCopy = (Node) copyObject(source);
               String destarr[] = targetPath.split("/");

               //test if the target path is an existing file or directory
               Node target = pathToNode(targetPath, false);
               if(target != null){
                    //target path is an existing file/directory
                    if(target.fileDesc.type == DIRECTORY){
                         //if the path is an existing directory, copy the source file/directory inside the target destination
                         sourceCopy.parent = target;
                         target.children.add(sourceCopy);
                    }
                    else if(target.fileDesc.type == FILE && sourceCopy.fileDesc.type == FILE){
                         //the target is an existing file like the source. overwrite the target file with the source copy
                         sourceCopy.parent = target.parent;
                         String targetOrigName = target.fileDesc.fileName;
                         target.fileDesc = sourceCopy.fileDesc;
                         target.fileDesc.fileName = targetOrigName;
                    }
                    else if(target.fileDesc.type != sourceCopy.fileDesc.type){
                         //the found target is a file and the source is a directory
                         //copy as if the target does not exist
                         sourceCopy.parent = target.parent;
                         target.parent.children.add(sourceCopy);
                    }
               }
               else{
                    sourceCopy.fileDesc.fileName = destarr[destarr.length - 1];
                    //target path does not exist, test if the parent of the target path is valid
                    Node targetDirectory = pathToNode(targetPath, true);
                    if(targetDirectory != null){
                         //parent of target path is valid, create a copy inside
                         sourceCopy.parent = targetDirectory;
                         targetDirectory.children.add(sourceCopy);
                    }
                    else //parent target path is unreachable/invalid
                         output(operation + ": " + targetPath + ": No such file/directory.");
               }
          }
          else //source file/destination specified does not exist
               output(operation + ": " + sourcePath + ": No such file/directory.");
     }

     public boolean isPath(String input){
          for(int i = 0; i < input.length(); i++){
               if(input.charAt(i) == '/')
                    return true;
          }
          return false;
     }

     public Node pathToNode(String absPath, boolean backDir){
          String pathArr[] = absPath.split("/");
          int i = 0, limit = pathArr.length;
          if(backDir)
               limit--;
          while(pathArr[i].equals("") || pathArr[i].equals(" "))
               i++;
          Node n = null;
          if(pathArr[i].equals("root") && absPath.charAt(0) == '/'){ n = tree.root; i++; }
          else{ n = tree.currentDirectory; }

          for(; i < limit; i++){
               boolean pathFound = false;
               if(pathArr[i].equals("..")){
                    //output("Go up..");
                    if(n.parent == null){
                         return null;
                    }
                    n = n.parent;
                    pathFound = true;
               }
               else{
                    //output("Searching for " + pathArr[i] + " in " + n.fileDesc.fileName);
                    for(int j = 0; j < n.children.size(); j++){
                         if(n.children.get(j).fileDesc.fileName.equals(pathArr[i])){
                              n = n.children.get(j);
                              pathFound = true;
                              break;
                         }
                    }
               }
               if(!pathFound)
                    return null;
          }
          return n;
     }

     public Object copyObject(Object objSource) {
        Object objDest = new Object();
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(objSource);
            oos.flush();
            oos.close();
            bos.close();
            byte[] byteData = bos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(byteData);
            try {
                objDest = new ObjectInputStream(bais).readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return objDest;
     }

     void output(String s){
          System.out.println(s);
          fileTextBuffer = fileTextBuffer + s + "\n";
     }

     static class CommandRegexList{
          public CommandRegex cmdrgx[];
          CommandRegexList(){
               CommandRegex tmp[] = { new CommandRegex("\\bprint\\b", "print tree") //print
                                    , new CommandRegex("\\bsave\\b", "save tree") //save
                                    , new CommandRegex("ls+[ ]*+/+[a-z 0-9 !#?.-]", "list contents absolute") //ls /root/myfolder
                                    , new CommandRegex("\\bls\\b", "list contents") //ls
                                    , new CommandRegex("mkdir+[ ]+[/]*+[a-z 0-9 !#?.-]", "make directory") //mkdir myfolder
                                    , new CommandRegex("cd+[ ]+[/]*+[a-z 0-9 !#?.-]", "change directory") //cd myfolder
                                    , new CommandRegex("rmdir+[ ]+[/]*+[a-z 0-9 !#?.-]", "remove directory") //rmdir myfolder
                                    , new CommandRegex("rn+[ ]+[/]*+[a-z 0-9 !#?/.-]+[ ]+[/]*+[a-z 0-9 !#?/.-]", "rename file/directory") //rn myfolder myfolder2
                                    , new CommandRegex("cp+[ ]+[/]*+[a-z 0-9 !#?/.-]+[ ]+[/]*+[a-z 0-9 !#?/.-]", "copy file/directory") //cp myfolder myfolder2
                                    , new CommandRegex("mv+[ ]+[/]*+[a-z 0-9 !#?/.-]+[ ]+[/]*+[a-z 0-9 !#?/.-]", "move file/directory") //mv myfolder myfolder2
                                    , new CommandRegex(">>+[ ]+[/]*+[a-z 0-9 !#?.-]", "append to file") //>> file.txt
                                    , new CommandRegex("edit+[ ]+[/]*+[a-z 0-9 !#?.-]", "edit file") //edit file.txt
                                    , new CommandRegex(">+[ ]+[/]*+[a-z 0-9 !#?.-]", "create a file") //> file.txt
                                    , new CommandRegex("rm+[ ]+[/]*+[a-z 0-9 !#?.-]", "remove file") //rm file.txt
                                    , new CommandRegex("show+[ ]+[/]*+[a-z 0-9 !#?.-]", "show file") //show file.txt
                                    , new CommandRegex("whereis+[ ]+[/]*+[a-z 0-9 !#?.-]", "search file/directory") //whereis file.txt
                                    , new CommandRegex("\\bwhereis\\b+[ ]+[*]+[ ]*", "search file/directory") //ls
                                    //show usage commands
                                    , new CommandRegex("mkdir", "mkdir usage") //mkdir
                                    , new CommandRegex("cp", "cp usage") //cp
                                    , new CommandRegex("mv", "mv usage") //mv
                                    , new CommandRegex("cd", "cd usage") //cd
                                    , new CommandRegex("rmdir", "rmdir usage") //rmdir
                                    , new CommandRegex("rm", "rm usage") //rm
                                    , new CommandRegex(">>", ">> usage") //>>
                                    , new CommandRegex(">", "> usage") //>
                                    , new CommandRegex("show", "show usage") //show
                                    , new CommandRegex("whereis", "whereis usage") //whereis
                                   };
               cmdrgx = tmp;
          }
     }

     static class CommandRegex{
          String regex;
          String meaning;
          CommandRegex(String r, String m){
               regex = r;
               meaning = m;
          }
     }
}

class Tree implements Serializable{
     final int FILE = 0, DIRECTORY = 1;
     Node root;
     Node currentDirectory;
     Canupas3 cmd;

     Tree(Canupas3 c){
          root = new Node(new FileDescriptor("root", DIRECTORY), null);
          currentDirectory = root;
          cmd = c;
     }

     void changeDirectory(String dirName){
          boolean dirExists = false;
          for(int i = 0; i < currentDirectory.children.size(); i++){
               if(currentDirectory.children.get(i).fileDesc.fileName.equals(dirName)
               && currentDirectory.children.get(i).fileDesc.type == DIRECTORY){
                    dirExists = true;
                    currentDirectory = currentDirectory.children.get(i);
                    break;
               }
          }
          if(!dirExists)
               cmd.output("No such directory in the current directory exists.");
     }

     void navToRoot(){
          currentDirectory = root;
     }

     void navUp(){
          if(currentDirectory == root){
               cmd.output("Invalid command. Already in the root directory!");
               return;
          }
          currentDirectory = currentDirectory.parent;
     }

     void addNode(Node n){
          n.parent = currentDirectory;
          currentDirectory.children.add(n);
     }

     void removeNode(Node del){
          for(int i = 0; i < currentDirectory.children.size(); i++){
               if(currentDirectory.children.get(i).equals(del))
                    currentDirectory.children.remove(i);
          }
     }

     boolean searchNode(String name){
          return search(name, root, "");
     }

     boolean search(String name, Node dir, String path){
          String rgx = name.replace("*", "\\w*");
          rgx = rgx.replace(".", "\\.");
          //check if the node is within the directory dir
          boolean exists = false;
          String orpath = path;
          for(int i = 0; i < dir.children.size(); i++){
               if(dir.children.get(i).fileDesc.fileName.matches(rgx)){
                    path = path + "/" + dir.fileDesc.fileName + "/" + dir.children.get(i).fileDesc.fileName;
                    cmd.output(path);
                    exists |= true;
               }
          }
          //if it is not in the 'dir' directory, do a recursive search on all the subdirectories of dir
          path = orpath;
          for(int i = 0; i < dir.children.size(); i++){
               if(dir.children.get(i).fileDesc.type == DIRECTORY)
                    exists |= search(name, dir.children.get(i), path + "/" + dir.fileDesc.fileName);
          }
          return exists;
     }

     String currentPath(){
          String path = currentDirectory.fileDesc.fileName;
          if(currentDirectory != root){
               Node n = currentDirectory.parent;
               while(n != null){
                    path = n.fileDesc.fileName + "/" + path;
                    n = n.parent;
               }
          }

          return path;
     }

     void listSubdirs(Node n){
          for(int i = 0; i < n.children.size(); i++){
               cmd.output(n.children.get(i).fileDesc.fileName);
          }
     }

     void print(){
          System.out.println("------------------------");
          printTree(root, 0);
          System.out.println("\n------------------------");
     }

     void printTree(Node root, int depth){
          if(depth > 0)
               System.out.println();
          for(int d = 0; d < depth; d++){
               System.out.print("    ");
          }
          System.out.print(root.fileDesc.fileName);
          if(root.fileDesc.type == DIRECTORY)
               System.out.print("/");
          depth++;
          for(int i = 0; i < root.children.size(); i++){
               printTree(root.children.get(i), depth);
          }
     }
}

class Node implements Serializable, Cloneable{
     public ArrayList<Node> children;
     public FileDescriptor fileDesc;
     public Node parent;

     Node(FileDescriptor fd){
          fileDesc = fd;
          parent = null;
          children = new ArrayList<Node>();
     }
     Node(FileDescriptor fd, Node p){
          parent = p;
          fileDesc = fd;
          children = new ArrayList<Node>();
     }
     Node(Node n){
          fileDesc = n.fileDesc;
          parent = n.parent;
          children = n.children;
     }
}

class FileDescriptor implements Serializable{
     String fileName, content;
     public int type;

     FileDescriptor(String name, int t){
          fileName = name;
          type = t;
          content = "";
     }
}
