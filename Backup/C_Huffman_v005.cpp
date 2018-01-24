#include <iostream>
#include <fstream>
#include <string>
#include <map>
#include <queue>
#include <list>

using namespace std;

string huffCodes[100];
char huffAB[100];
int hfIndex = 0;

// for saving huffman table for position and length
string huffCodesPL[10];
char huffABPL[10];
string EncodedHTree, EncodedHTree01, EncodedHTree02;

// for saving huffman table for position and length
string huffCodesC[100];
char huffABC[100];

fstream huffmanFile;
string fileNameH;
list<string> positionsChar;
list<string> lengthsChar;
list<char> charsChar;
map<char,int> charMap;
map<char,int> posLenMap;

// A Huffman tree node
struct node {
    
    char data;
    int freq;
    
    node *left, *right;
    
    node(char data, unsigned freq)
    
    {
        left = right = NULL;
        this->data = data;
        this->freq = freq;
    }
};

struct compare {
    
    bool operator()(node* l, node* r)
    
    {
        return (l->freq > r->freq);
    }
};

void getCodes(struct node* root, string str, string eHuffmanTree)
{
    if (!root){
        eHuffmanTree.resize(eHuffmanTree.length() -1);
        return;
    }
    
    if (root->data != '$')
    {
        eHuffmanTree.resize(eHuffmanTree.length() -1);
        eHuffmanTree += "1";
        eHuffmanTree += root->data;
        EncodedHTree += eHuffmanTree;
        huffCodes[hfIndex] = str;
        //cout << root->data << ": " << str << "\n";
        huffAB[hfIndex] = root->data;
        hfIndex++;
    }
    //else eH.resize(eH.length() -1);
    
    getCodes(root->left, str + "1", eHuffmanTree + "0");
    eHuffmanTree = "";
    getCodes(root->right, str + "0", eHuffmanTree +"0");
}

void HuffmanCalc(char data[], int freq[], int size)
{
    struct node *left, *right, *top;
    
    priority_queue<node*, vector<node*>, compare> minHeap;
    
    for (int i = 0; i < size; ++i)
        minHeap.push(new node(data[i], freq[i]));
    
    while (minHeap.size() != 1) {
        
        left = minHeap.top();
        minHeap.pop();
        
        right = minHeap.top();
        minHeap.pop();
        
        top = new node('$', left->freq + right->freq);
        
        top->left = left;
        top->right = right;
        
        minHeap.push(top);
    }
    getCodes(minHeap.top(), "", "0");
}

bool OpenFile(string openthis, fstream& file_open, string& fileArray);

bool ReadFile(int &counter, int &mapSizeLenPos, int &mapSizeC)
{
    pair<map<char,int>::iterator, bool> retChar;
    pair<map<char,int>::iterator, bool> ret2;
    
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
                
                retChar = posLenMap.insert(pair<char,int>(lookAhead, 1));
                if (retChar.second == false) retChar.first->second++;
                
                huffmanFile.get(lookAhead);
            }
            huffmanFile.get(lookAhead);
            while (lookAhead != ')') {
                length += lookAhead;
                
                retChar = posLenMap.insert(pair<char,int>(lookAhead, 1));
                if (retChar.second == false) retChar.first->second++;
                
                huffmanFile.get(lookAhead);
            }
            huffmanFile.get(lookAhead);
            
            // save all the positions to be used later in the creation of the final file
            // to better differenciate the values from each other, all position has two digits
            //            if (position.length() < 2) positionsChar.push_back('0' + position);
            //            else positionsChar.push_back(position);
            //
            //            if (length.length() < 2) lengthsChar.push_back('0' + length);
            //            else lengthsChar.push_back(length);
            
            character = lookAhead;
            
            ret2 = charMap.insert(pair<char,int>(character, 1));
            if (ret2.second == false) ret2.first->second++;
            
            charsChar.push_back(character);
            
            counter++;
        }
    }
    mapSizeLenPos = posLenMap.size();
    mapSizeC = charMap.size();
    huffmanFile.close();
    return true;
}

void writeHT(string eHFT, ostream &output)
{
    char buffer = NULL;
    int currentBit = 0;
    char tempChar = NULL;
    for (int i = 0; i < eHFT.length(); i++) {
        if (eHFT[i] == '0') {
            buffer |= (false << (7 - currentBit));
            currentBit++;
        }
        else if (eHFT[i] == '1')
        {
            char C;
            buffer |= (true << (7 - currentBit));
            currentBit++;
            i++;
            if (currentBit == 8) {
                output.write(&buffer, 1);
                currentBit = 0;
                buffer = NULL;
            }
            C = eHFT[i];
            tempChar = eHFT[i] >> currentBit;
            cout << eHFT[i];
            buffer |= tempChar;
            output << buffer;
            buffer = NULL;
            C = eHFT[i];
            tempChar = eHFT[i] << (7 - currentBit +1);
            buffer |= tempChar;
            //currentBit = 7 - currentBit;
            
        }
        if (currentBit == 8) {
            output.write(&buffer, 1);
            currentBit = 0;
            buffer = NULL;
        }
    }
    if (currentBit != 0) output.write(&buffer, 1);
    
}

void writePositionLength(list<string> listPL, ofstream &output)
{
    char buffer = NULL;
    int currentBit = 0;
    
    for (std::list<string>::iterator it=listPL.begin(); it!=listPL.end(); ++it)
    {
        string temp = *it;
        string temp1, temp2;
        for (int i = 0; i < 10; i++) {
            if (temp[0] == huffABPL[i]) {
                temp1 = huffCodesPL[i];
            }
            if (temp[1] == huffABPL[i]) {
                temp2 = huffCodesPL[i];
            }
        }
        // write output binary values
        //cout << temp1 << temp2;
        
        for (int i = 0; i < temp1.length(); i++) {
            if (temp1[i] == '0') {
                buffer |= (false << (7 - currentBit));
                currentBit++;
            }
            else{
                buffer |= (true << (7 - currentBit));
                currentBit++;
            }
            if (currentBit == 8) {
                output.write(&buffer, 1);
                currentBit = 0;
                buffer = NULL;
            }
        }
        for (int i = 0; i < temp2.length(); i++) {
            if (temp2[i] == '0') {
                buffer |= (false << (7 - currentBit));
                currentBit++;
            }
            else{
                buffer |= (true << (7 - currentBit));
                currentBit++;
            }
            if (currentBit == 8) {
                output.write(&buffer, 1);
                currentBit = 0;
                buffer = NULL;
            }
        }
        
        //cout << temp1 << temp2;
    }
    if (currentBit != 0) output.write(&buffer, 1);
}

void writeChar(list<char> listC, ofstream &output, int charHSize)
{
    char buffer = NULL;
    int currentBit = 0;
    for (std::list<char>::iterator it=listC.begin(); it!=listC.end(); ++it)
    {
        char temp = *it;
        string temp1;
        for (int i = 0; i <= charHSize; i++) {
            if (temp == huffABC[i]) {
                temp1 = huffCodesC[i];
            }
        }
        // write output binary values
        //cout << temp1;
        for (int i = 0; i < temp1.length(); i++) {
            if (temp1[i] == '0') {
                buffer |= (false << (7 - currentBit));
                currentBit++;
            }
            else{
                buffer |= (true << (7 - currentBit));
                currentBit++;
            }
            if (currentBit == 8) {
                output.write(&buffer, 1);
                currentBit = 0;
                buffer = NULL;
            }
        }
    }
    if (currentBit != 0) output.write(&buffer, 1);
}


bool WriteOutput(int count, int charHSize)
{
    // create the output file variable and the bool to verify endoffile, and open the file that will hold the decompressed information
    ofstream output;
    // after this path will follow the path like in compression
    output.open("/Users/eliaspiovani/Documents/OneDrive/Projects/Programming/Codec_Project/Codec/resources/output.hfm", fstream::binary);
    // write headers to the file, alphabet and frequencies, and the total count
    
    char charSize = charHSize;
    output << '#' << count << '#';
    output.write(&charSize, 1);
    output << '#';
    
    writeHT(EncodedHTree01, output);
    writeHT(EncodedHTree02, output);
    
    output << '#';
    
    writePositionLength(positionsChar, output);
    writePositionLength(lengthsChar, output);
    
    writeChar(charsChar, output, charHSize);
    
    return true;
}

bool C_Huffman()
{
    int count = 0;
    int sizeOfMapPL;
    int sizeOfMapC;
    // read lz77 file and get all the values of position, length and newChar
    ReadFile(count, sizeOfMapPL, sizeOfMapC);
    
    char alphabet[sizeOfMapPL];
    int freq[sizeOfMapPL];
    
    int i = 0;
    map<char,int>::iterator it = posLenMap.begin();
    for (it = posLenMap.begin(); it != posLenMap.end(); ++it, i++)
    {
        alphabet[i] = it->first;
        freq[i] = it->second;
        //cout << it->first << '\t' << it->second << '\n';
    }
    
    int size = sizeof(alphabet) / sizeof(alphabet[0]);
    EncodedHTree = "";
    HuffmanCalc(alphabet, freq, size);
    
    EncodedHTree01 = EncodedHTree;
    //cout << "Huffman Position + Length" << endl;
    for (int i = 0; i < size; i++) {
        //cout << huffAB[i] << '\t' << huffCodes[i] << endl;
        huffABPL[i] = huffAB[i];
        huffCodesPL[i] = huffCodes[i];
    }
    
    // huffman for new characters
    
    char alphabetChar[sizeOfMapC];
    int freqChar[sizeOfMapC];
    
    i = 0;
    map<char,int>::iterator it2 = charMap.begin();
    for (it2 = charMap.begin(); it2 != charMap.end(); ++it2, i++)
    {
        alphabetChar[i] = it2->first;
        freqChar[i] = it2->second;
        //cout << it->first << '\t' << it->second << '\n';
    }
    
    size = sizeof(alphabetChar) / sizeof(alphabetChar[0]);
    EncodedHTree = "";
    hfIndex = 0;
    HuffmanCalc(alphabetChar, freqChar, size);
    EncodedHTree02 = EncodedHTree;
    //cout << "Huffman Characters" << endl;
    for (int i = 0; i < size; i++) {
        //cout << huffAB[i] << '\t' << huffCodes[i] << endl;
        huffABC[i] = huffAB[i];
        huffCodesC[i] = huffCodes[i];
    }
    
    WriteOutput(count, size);
    
    return true;
}

