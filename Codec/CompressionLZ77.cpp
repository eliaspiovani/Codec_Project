#include <iostream>
#include <fstream>
#include <list>

using namespace std;

#define maxLength 99
#define extension "lz77"

fstream File;
string fileName;
//string outputPath;
//const string extension = "lz77";

bool OpenFile(string openthis, fstream& file_open, string& fileArray);

//File compression
bool CompressionLZ77 (const int fixedDictLength)
{
    // call function to open the file that will be compressed
    if (!OpenFile("java", File, fileName)) {
        cerr << "File not found" << endl;
        return false;
    }
    
    //create the slider windown and the iterator of it
    list<char> sWindown;
    list<char>::iterator it;
    
    // variable for the output file and a bool to verify if it is the end of the file
    ofstream output;
    bool endOfFile = false;
    
    // filename contains the path + name of file without extension
    output.open(fileName + extension);
    if (!output) {
        cerr << "Error creating output file";
        return false;
    }
    
    //loop for searching through all the file
    while (!endOfFile){
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
                
                // verify if next character in windown is also equal to the next character in lookahead
                // until the maximum length for sequential characters
                for (int i = 0; i < maxLength && it != sWindown.end(); i++) {
                    --it;
                    if (lookAhead == *it && !File.eof()) {
                        length++;
                        sWindown.push_front(lookAhead);
                        File.get(lookAhead);
                    }
                    else if (File.eof()) endOfFile = true;
                    else if (lookAhead != *it) break;
                }
                
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
    
    // print search windown on console
    /*cout << "Search Windown:" << endl;
    for (it = sWindown.begin(); it != sWindown.end(); ++it) {
        cout << *it;
    }
    cout << endl;*/
    
    output.close();
    File.close();
    return true;
}

