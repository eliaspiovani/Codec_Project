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

fstream huffmanFile;
string fileNameH;
list<char> positionsChar;

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

void getCodes(struct node* root, string str)
{
    if (!root)
        return;
    
    if (root->data != '$')
    {
        huffCodes[hfIndex] = str;
        //cout << root->data << ": " << str << "\n";
        huffAB[hfIndex] = root->data;
        hfIndex++;
    }
    
    getCodes(root->left, str + "1");
    getCodes(root->right, str + "0");
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
    getCodes(minHeap.top(), "");
}

bool OpenFile(string openthis, fstream& file_open, string& fileArray);

bool ReadFile()
{
    
    
    return true;
}

bool C_Huffman()
{
    pair<map<char,int>::iterator, bool> retChar;
    pair<map<char,int>::iterator, bool> ret2;
    map<char,int> charMap;
    map<char,int> posLenMap;
    int count = 0;
    
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
            character = lookAhead;
            
            
            ret2 = charMap.insert(pair<char,int>(character, 1));
            if (ret2.second == false) ret2.first->second++;
            
            count++;
        }
    }
    
    char alphabet[posLenMap.size()];
    int freq[posLenMap.size()];
    
    //alphabet[0] = ',';
    //freq[0] = count*2;
    
    int i = 0;
    map<char,int>::iterator it = posLenMap.begin();
    for (it = posLenMap.begin(); it != posLenMap.end(); ++it, i++)
     {
         alphabet[i] = it->first;
         freq[i] = it->second;
         //cout << it->first << '\t' << it->second << '\n';
     }
    
    
    
    int size = sizeof(alphabet) / sizeof(alphabet[0]);
    
    HuffmanCalc(alphabet, freq, size);
    
    huffmanFile.clear();
    huffmanFile.seekg(0, ios::beg);
    
    
    for (int i = 0; i < size; i++) {
        cout << huffAB[i] << '\t' << huffCodes[i] << endl;
    }
    
    
    
    
    huffmanFile.close();
    return true;
}
