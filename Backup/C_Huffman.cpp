#include <iostream>
#include <fstream>
#include <list>
#include <map>
#include <vector>

using namespace std;

fstream huffmanFile;
string fileNameH;

bool OpenFile(string openthis, fstream& file_open, string& fileArray);

bool C_Huffman()
{
    list<int> tempList, xValues, yValues;
    list<int>::iterator itList = tempList.begin();
    list<string> xSValues;
    
    pair<map<string,int>::iterator, bool> ret1;
    pair<map<char,int>::iterator, bool> retChar;
    
    map<string,int> posMap;
    map<string,int> lenMap;
    map<string,int> pos_lenMap;
    map<char,int> charMap;
    map<char,int> lengthMap;
    map<char,int> positionMap;
    
    map<string,int>::iterator it = posMap.begin();
    //map<char,int>::iterator it2 = charMap.begin();
    //map<string,int>::iterator it3 = pos_lenMap.begin();
    int count = 0;
    
    //map<int,string> xMap;
    //map<int,string>::iterator it_ = xMap.begin();
    
    // call function to open the file that will be compressed
    if (!OpenFile("lz77", huffmanFile, fileNameH)) {
        cerr << "File not found" << endl;
        return false;
    }
    
    bool endOfFile = false;
    
    
    
    while (!endOfFile){
        char lookAhead;
        string position, length = "";
        char character;
        
        
        
        
        if (!huffmanFile.eof()) huffmanFile.get(lookAhead);
        else break;
        
        if (lookAhead == '(') {
            huffmanFile.get(lookAhead);
            while (lookAhead != ',') {
                position += lookAhead;
                
                retChar = positionMap.insert(pair<char,int>(lookAhead, 1));
                if (retChar.second == false) retChar.first->second++;
                
                huffmanFile.get(lookAhead);
            }
            huffmanFile.get(lookAhead);
            while (lookAhead != ')') {
                length += lookAhead;
                
                retChar = lengthMap.insert(pair<char,int>(lookAhead, 1));
                if (retChar.second == false) retChar.first->second++;

                huffmanFile.get(lookAhead);
            }
            huffmanFile.get(lookAhead);
            character = lookAhead;
            
            
            ret1 = posMap.insert(pair<string,int>(position, 1));
            if (ret1.second == false) {
                ret1.first->second++;
            }
            
            //pair<map<string,int>::iterator, bool> ret2;
            ret1 = lenMap.insert(pair<string,int>(length, 1));
            if (ret1.second == false) {
                ret1.first->second++;
            }
            
            pair<map<char,int>::iterator, bool> ret2;
            ret2 = charMap.insert(pair<char,int>(character, 1));
            if (ret2.second == false) ret2.first->second++;
            
//            ret1 = pos_lenMap.insert(pair<string,int>(position, 1));
//            if (ret1.second == false) {
//                ret1.first->second++;
//            }
//            ret1 = pos_lenMap.insert(pair<string,int>(length, 1));
//            if (ret1.second == false) {
//                ret1.first->second++;
//            }
            
            count++;
            
            //cout << character;
        }
        
        
    }
    /*for (it=posMap.begin(), i=0; it!=posMap.end(); ++it, i++)
    {
        
        cout << i << '\t' << it->first << '\t' << it->second << '\n';
    }*/
    cout << endl;
    for (it=lenMap.begin(); it!=lenMap.end(); ++it)
    {
        //tempList.insert(itList, it->second);
        tempList.push_front(it->second);
        yValues.push_front(it->second);
        
        cout << it->first << '\t' << it->second << '\n';
    }
    cout << endl;
    
//    for (retChar=charMap.begin(); retChar!=charMap.end(); ++retChar)
//        cout << i << '\t' << retChar->first << '\t' << retChar->second << '\n';
    cout << endl;
//    for (it3=pos_lenMap.begin(), i=0; it3!=pos_lenMap.end(); ++it3, i++)
//        cout << i << '\t' << it3->first << '\t' << it3->second << '\n';
    
    cout << count << endl;
    
    /*for (itList=mylist.begin(); itList!=mylist.end(); ++itList)
        cout << ' ' << *itList;
    cout << endl;*/
    
    /*for (itList=tempList.begin(); itList!=tempList.end(); ++itList)
        cout << ' ' << *itList;
    cout << endl;*/
        
    tempList.sort();
    int result = 0;
    while (result < count) {
        result = tempList.front();
        tempList.pop_front();
        result += tempList.front();
        tempList.pop_front();
        tempList.insert(itList, result);
        tempList.sort();
        //cout << result << " ";
        
        xValues.push_front(result);
        yValues.push_front(result);
        xSValues.push_front(to_string(result) + ".i");
    }
//    yValues.sort();
//    yValues.pop_back();
//    for (itList=yValues.begin(); itList!=yValues.end(); ++itList)
//     cout << ' ' << *itList;
//     cout << endl;
//    xValues.sort();
//    for (itList=xValues.begin(); itList!=xValues.end(); ++itList)
//        cout << ' ' << *itList;
//    cout << endl;
    
    
//    string tempSList[xValues.size()];
//    int i = xValues.size();
//    for (itList = yValues.end(); itList != yValues.begin(); --itList) {
//        <#statements#>
//    }
    
    
    
    
    huffmanFile.close();
    return true;
}
