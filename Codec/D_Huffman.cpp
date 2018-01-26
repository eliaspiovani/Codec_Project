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

using namespace std;

int number = 0;


void readHT(int& i, int length, const string& eHFT, string code)
{
    if (number == length) {
        return;
    }
    if (eHFT[i] == '1') {
        cout << code << endl;
        ++i;
        number++;
        return;
    }
    readHT(++i, length, eHFT, code + '1');
    readHT(++i, length, eHFT, code + '0');
}

void createHT(const char eHF[], const int& length)
{
    unsigned int currentBit = 7, i = 0;
    char buffer, temp;
    string eHFT;
    
    while (i <= length +1) {
        
        if (!((eHF[i] >> currentBit) & true)) {
            cout << ((eHF[i] >> currentBit) & true);
            currentBit--;
            eHFT += "0";
        }
        else
        {
            cout << ((eHF[i] >> currentBit) & true);
            eHFT += "1";
            currentBit--;
            if (currentBit > 7) {
                currentBit = 7;
                i++;
            }
            buffer = NULL;
            buffer = eHF[i] << (7 - currentBit);
            i++;
            //temp = eHF[i];
            //temp2 = (CHAR_MAX >> (currentBit));
            temp = (eHF[i] >> (currentBit +1)) & (CHAR_MAX >> (currentBit));
            //temp &= temp2;
            buffer |= temp;
            eHFT += buffer;
            cout << "(" << buffer << ")";
            
            //currentBit++;
        }
        if (currentBit > 7) {
            currentBit = 7;
            i++;
        }
    }
    cout << endl;
    
    string code;
    int hIndex = 0;
    readHT(hIndex, length, eHFT, "");
    
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
    
    createHT(eHTpl, temp[2]);
    createHT(eHTc, temp[3]);
    
    
    
    input.close();
    
    return true;
}
