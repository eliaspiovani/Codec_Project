//
//  D_Huffman.cpp
//  Codec
//
//  Created by Elias Piovani on 25/01/2018.
//  Copyright © 2018 Elias Piovani. All rights reserved.
//

#include <iostream>
#include <fstream>
#include <math.h>
#include <map>
#include <bitset>
#include <list>

using namespace std;

int number = 0;
typedef map<string, char> hfMap;
typedef list<string> blockList;
hfMap plMap;
hfMap cMap;

int getMaxLength(const hfMap& tempMap)
{
    int maxLength = 0;
    
    for (hfMap::const_iterator it=tempMap.begin(); it!=tempMap.end(); ++it)
    {
        if (it->first.length() > maxLength) maxLength = (int)it->first.length();
    }
    
    return maxLength;
}

void saveToListPL(blockList& list, string& body, const int& maxLength)
{
    string temp;
    int nComma = 0;
    for (int j = 0; j < body.length(); j++) {
        string substring;
        hfMap::const_iterator it2;
        
        for (int i = 1; i <= maxLength && nComma < 2; i++) {
            substring = body.substr(j, i);
            it2 = plMap.find(substring);
            
            if (it2 != plMap.end()) {
                
                
                if (it2->second == ',') {
                    nComma++;
                    if (nComma == 2) {
                        body.erase(0, j+i);
                        break;
                    }
                    else
                    {
                        list.push_back(temp);
                        temp = "";
                    }
                }
                else
                {
                    //cout << it2->second;
                    temp += it2->second;
                    nComma = 0;
                }
                
                j += i-1;
                break;
            }
        }
    }
    //cout << "break" << endl;
}

void saveToListnC(blockList& list, string& body, const int& maxLength)
{
    string temp;
    for (int j = 0; j < body.length(); j++) {
        string substring;
        hfMap::const_iterator it2;
        
        for (int i = 1; i <= maxLength; i++) {
            substring = body.substr(j, i);
            it2 = cMap.find(substring);
            
            if (it2 != cMap.end()) {
                temp = it2->second;
                list.push_back(temp);
                
//                if (it2->second == ',') {
//                    nComma++;
//                    if (nComma == 2) {
//                        body.erase(0, j+i);
//                        break;
//                    }
//                    else
//                    {
//                        list.push_back(temp);
//                        temp = "";
//                    }
//                }
//                else
//                {
//                    cout << it2->second;
//                    temp += it2->second;
//                    nComma = 0;
//                }
                
                j += i-1;
                break;
            }
        }
    }
    //cout << "break" << endl;
}

void decodeBody(string& body)
{
    string temp;
    blockList listPosition, listLength, listnChar;
    //int nComma = 0;
    int maxPLccodeLength = getMaxLength(plMap);
    int maxCcodeLength = getMaxLength(cMap);
    
    //cout << maxPLccodeLength << " " << maxCcodeLength << endl;
    
    saveToListPL(listPosition, body, maxPLccodeLength);
    saveToListPL(listLength, body, maxPLccodeLength);
    saveToListnC(listnChar, body, maxCcodeLength);
    
    while (!listPosition.empty())
    {
        cout << "(" << listPosition.front()<< "," << listLength.front() << ")" << listnChar.front();
        listPosition.pop_front();
        listLength.pop_front();
        listnChar.pop_front();
    }
    
    //for (string::const_iterator it = body.begin(); it != body.end(); ++it) {
//    for (int j = 0; j < body.length(); j++) {
//        string substring;
//        hfMap::const_iterator it2;
//
//        for (int i = 1; i <= maxPLccodeLength && nComma < 2; i++) {
//            substring = body.substr(j, i);
//            it2 = plMap.find(substring);
//
//            if (it2 != plMap.end()) {
//
//
//                if (it2->second == ',') {
//                    nComma++;
//                    if (nComma == 2) {
//                        body.erase(0, j);
//                        break;
//                    }
//                    else
//                    {
//                        list.push_back(temp);
//                        temp = "";
//                    }
//                }
//                else
//                {
//                    cout << it2->second;
//                    temp += it2->second;
//                    nComma = 0;
//                }
//
//                j += i-1;
//                break;
//            }
//        }
    //    }
}

void readHT(int& i, int length, const string& eHFT, string code, hfMap& tempMap)
{
    // if the number of leaves is equal to length, it has finish so return
    if (number >= length) {
        return;
    }
    // if the node has number one it means the next char is a leaf, read the live and get the code
    if (eHFT[i] == '1') {
        ++i;
        //get char and code
        tempMap.insert(pair<string, char>(code, eHFT[i]));
        //cout << code << endl;
        
        number++;
        return;
    }
    // read left node adding 1 to the code
    readHT(++i, length, eHFT, code + '1', tempMap);
    // read right node adding 0 to the code
    readHT(++i, length, eHFT, code + '0', tempMap);
}

void createHT(const char eHF[], const int& length, hfMap& tempMap)
{
    unsigned int currentBit = 7, i = 0;
    char buffer, temp;
    string eHFT;
    
    int totalLength = ceil((length * 8 + length + length - 1)/8.0);
    
    while (i <= totalLength) {
        
        if (!((eHF[i] >> currentBit) & true)) {
            //cout << ((eHF[i] >> currentBit) & true);
            currentBit--;
            eHFT += "0";
        }
        else
        {
            //cout << ((eHF[i] >> currentBit) & true);
            eHFT += "1";
            currentBit--;
            if (currentBit > 7) {
                currentBit = 7;
                i++;
            }
            buffer = NULL;
            buffer = eHF[i] << (7 - currentBit);
            i++;
            temp = (eHF[i] >> (currentBit +1)) & (CHAR_MAX >> (currentBit));
            buffer |= temp;
            eHFT += buffer;
            //cout << "(" << buffer << ")";
            
            //currentBit++;
        }
        if (currentBit > 7) {
            currentBit = 7;
            i++;
        }
    }
    //cout << endl;
    
    string code;
    int hIndex = 0;
    number = 0;
    readHT(hIndex, length, eHFT, "", tempMap);
    
}


bool D_Huffman()
{
    char temp[4];
    int counter = 0, eHTpl_length = 0, eHTc_length = 0;
    
    ifstream input;
    input.open("/Users/eliaspiovani/Documents/OneDrive/Projects/Programming/Codec_Project/Codec/resources/output.hfm");
    
    if (!input.is_open()) {
        cerr << "File not found" << endl;
        return false;
    }
    
    //get fixed header from the file
    input.read(temp, 4);
    //the first two bytes contain information about the total number of tuples (pos,len)nextC
    counter = temp[0] << 8 | (temp[1] & 255);
    
    // the next two bytes contain information about the length of the alphabet for pos/len and nextC
    // to know the number of bytes to read use this formula : (length * 8bit + lentgh + length-1) / 8bit)
    // the result has to be rounded up
    eHTpl_length = ceil((temp[2] * 8 + temp[2] + temp[2] - 1)/8.0);
    char eHTpl[eHTpl_length];
    eHTc_length = ceil((temp[3] * 8 + temp[3] + temp[3] - 1)/8.0);
    char eHTc[eHTc_length];
    
    // read the encoded huffman tree used in position and length values
    input.read(eHTpl, eHTpl_length);
    // read the encoded huffman tree used in nextChar values
    input.read(eHTc, eHTc_length);
    
    createHT(eHTpl, temp[2], plMap);
    createHT(eHTc, temp[3], cMap);
    string bodyFile = "";
    
    // read the body of the file
    while (!input.eof()) {
        input.read(temp, 1);
        bodyFile += bitset<8>(temp[0]).to_string();
    }
    
    // decode the body according to the HF codes
    decodeBody(bodyFile);
    

    input.close();
    
    return true;
}
