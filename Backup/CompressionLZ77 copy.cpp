#include <iostream>
#include <fstream>
#include <list>
#include <string>

using namespace std;

fstream File;
string fileName;
string outputPath;
const string extension = "ccc";

// Path of the file to be compressed
string FilePath()
{
    string filePath = "/Users/eliaspiovani/Documents/OneDrive/Projects/Programming/Codec_Project/Codec/AbstractLoadingCacheTest.java";
    size_t found = filePath.find_last_of('/');
    
    outputPath = filePath.substr(0, found+1);
    fileName = filePath.substr(found +1);
    
    return filePath;
}

// Open file that will be compressed
bool OpenFile()
{
    File.open(FilePath());
    
    if (!File)
    {
        cerr << "Unable to open file" << endl;
        return false;
    }
    
    return true;
}

//File compression
bool CompressionLZ77 (const int fixedDictLength)
{
    // call function to open the file that will be compressed
    if (!OpenFile()) {
        cerr << "File not found" << endl;
        return false;
    }
    
    list<char> sWindown;
    list<char>::iterator it;
    ofstream output;
    bool endOfFile = false;
    
    size_t found = fileName.find_last_of('.');
    output.open(outputPath + fileName.substr(0, found +1) + extension);
    if (!output) {
        cerr << "Error creating output file";
        return false;
    }
    
    //loop for searching through all the file
    while (!endOfFile){
    //for (int i = 0; i <= 64; i++) {
        char lookAhead;
        
        if (!File.eof()) File.get(lookAhead);
        //else return true;
        else break;
        
        int windowPos = 0;
        int length = 1;
        bool isInWindow = false;
        
        //verify if contains this char in search windown
        // if contains, verify if next character is the same for both, if not continue searching better option
        // if better option was found return and verify next character...
        // in the end
        //write the position (position, length)next char and add char/chars to the slider windown
        
        for (it = sWindown.begin(), windowPos = 0; it != sWindown.end(); ++it, windowPos++) {
            
            // if search windown contain the character that is being compressed
            if (lookAhead == *it) {
                sWindown.push_front(lookAhead);
                
                
                //repeat block to find more sequence chars in search window
                if (!File.eof()) File.get(lookAhead);
                else endOfFile = true;
                
                //i++; // only if using for loop
                
                // verify if next character in windown is also equal to the next character in lookahead
                for (int i = 0; i < 10 && it != sWindown.end(); i++) {
                    --it;
                    if (lookAhead == *it && !File.eof()) {
                        length++;
                        sWindown.push_front(lookAhead);
                        File.get(lookAhead);
                    }
                    else if (File.eof()) endOfFile = true;
                    else if (lookAhead != *it) break;
                }
                /*--it;
                if (lookAhead == *it && !file.eof()) {
                    length++;
                    sWindown.push_front(lookAhead);
                    file.get(lookAhead);
                }
                else if (file.eof()) endOfFile = true;*/
                
                // print the tuple to the output file
                output << '(' << windowPos << "," << length << ")" << lookAhead;
                isInWindow = true;
                break;
            }
        }
        // if the character isn't in search windown then it is a new character, write the tuple for new characters in the output
        if (!isInWindow) {
            output << "(0,0)" << lookAhead;
        }
        
        //output << lookAhead;
        
        sWindown.push_front(lookAhead);
        // if list greater than 64, delete last char
        while (sWindown.size() > fixedDictLength) {
            sWindown.pop_back();
        }
    }
    
    cout << "Search Windown:" << endl;
    for (it = sWindown.begin(); it != sWindown.end(); ++it) {
        cout << *it;
    }
    cout << endl;
    
    output.close();
    return true;
}
