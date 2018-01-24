#include <iostream>
#include <fstream>
#include <list>

using namespace std;

#define dExtension "java"

fstream dFile;
string dFileName;
//const string dExtension = "java";

bool OpenFile(string openthis, fstream& file_open, string& fileArray);

bool DecompressionLZ77 (const int fixedDictLength)
{
    // call function to open the file that will be compressed
    if (!OpenFile("lz77", dFile, dFileName)) {
        cerr << "File not found" << endl;
        return false;
    }
    
    //search windown creation
    list<char> sWindown;
    list<char>::iterator it;
    
    // create the output file variable and the bool to verify endoffile, and open the file that will hold the decompressed information
    ofstream output;
    bool endOfFile = false;
    // after this path will follow the path like in compression
    output.open("/Users/eliaspiovani/Documents/OneDrive/Projects/Programming/Codec_Project/Codec/resources/output.java");
    
    //loop for searching through all the file
    while (!endOfFile){
        char lookAhead;
        string position, length = "";
        char character;
        
        if (!dFile.eof()) dFile.get(lookAhead);
        else break;
            
        if (lookAhead == '(') {
            dFile.get(lookAhead);
            while (lookAhead != ',') {
                position += lookAhead;
                dFile.get(lookAhead);
            }
            dFile.get(lookAhead);
            while (lookAhead != ')') {
                length += lookAhead;
                dFile.get(lookAhead);
            }
            dFile.get(lookAhead);
            character = lookAhead;
            
            if (position == "0" && length == "0") {
                output << character;
            }
            else if (length != "0")
            {
                it = sWindown.begin();
                for (int i = 0; i < stoi(position); i++) {
                    ++it;
                }
                for (int i = 0; i < stoi(length); i++) {
                    output << *it;
                    sWindown.push_front(*it);
                    --it;
                }
                output << character;
            }
            sWindown.push_front(character);
            
        }
        
    while (sWindown.size() > fixedDictLength) {
        sWindown.pop_back();
    }

    }
    
    // print search windown on console
    /*cout << "Search Windown:" << endl;
    for (list<char>::iterator it2 = sWindown.begin(); it2 != sWindown.end(); ++it2) {
        cout << *it2;
    }
    cout << endl;*/
    
    output.close();
    dFile.close();
    return true;
}
