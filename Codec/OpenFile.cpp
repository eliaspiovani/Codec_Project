#include <iostream>
#include <fstream>
//#include <list>

using namespace std;

string GetFileName(string openThis)
{
    string filePathComplete;
    string filePath;
    
    // File path will come from the UI
    filePathComplete = "/Users/eliaspiovani/Documents/OneDrive/Projects/Programming/Codec_Project/Codec/resources/OutputJAVA.java";
    
    size_t found = filePathComplete.find_last_of('.');
    filePath = filePathComplete.substr(0, found+1);
    
    return filePath;
}


bool OpenFile(string openthis, fstream& file_open, string& fileArray)
{
    fileArray = GetFileName(openthis);
    cout << fileArray + openthis << endl;
    
    file_open.open(fileArray + openthis);
    if (!file_open)
    {
        cerr << "Unable to open file" << endl;
        return false;
    }
    
    return true;
}
