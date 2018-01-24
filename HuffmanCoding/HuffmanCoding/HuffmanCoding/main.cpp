#include <string>
#include <iostream>
#include <queue>

using namespace std;

string huffCodes[] = {};
char huffAB[] = {};
int hfIndex = 0;

// A Huffman tree node
struct MinHeapNode {
    
    // One of the input characters
    char data;
    
    // Frequency of the character
    unsigned freq;
    
    // Left and right child
    MinHeapNode *left, *right;
    
    MinHeapNode(char data, unsigned freq)
    
    {
        left = right = NULL;
        this->data = data;
        this->freq = freq;
    }
};

// For comparison of
// two heap nodes (needed in min heap)
struct compare {
    
    bool operator()(MinHeapNode* l, MinHeapNode* r)
    
    {
        return (l->freq > r->freq);
    }
};

// Prints huffman codes from
// the root of Huffman Tree.
void getCodes(struct MinHeapNode* root, string str)
{
    if (!root)
        return;
    
    if (root->data != '$')
    {
        huffCodes[hfIndex] += root->data + str;
        //cout << root->data << ": " << str << "\n";
        //huffAB[hfIndex] = root->data;
        hfIndex++;
    }
    
    getCodes(root->left, str + "1");
    getCodes(root->right, str + "0");
}

// The main function that builds a Huffman Tree and
// print codes by traversing the built Huffman Tree
void HuffmanCalc(char data[], int freq[], int size)
{
    struct MinHeapNode *left, *right, *top;
    
    // Create a min heap & inserts all characters of data[]
    priority_queue<MinHeapNode*, vector<MinHeapNode*>, compare> minHeap;
    
    for (int i = 0; i < size; ++i)
        minHeap.push(new MinHeapNode(data[i], freq[i]));
    
    // Iterate while size of heap doesn't become 1
    while (minHeap.size() != 1) {
        
        // Extract the two minimum
        // freq items from min heap
        left = minHeap.top();
        minHeap.pop();
        
        right = minHeap.top();
        minHeap.pop();
        
        top = new MinHeapNode('$', left->freq + right->freq);
        
        top->left = left;
        top->right = right;
        
        minHeap.push(top);
    }
    // Print Huffman codes using
    // the Huffman tree built above
    getCodes(minHeap.top(), "");
}

// Driver program to test above functions
int main()
{
    
    char alphabet[] = { 'a', 'b', 'c', 'd', 'e'};
    int freq[] = { 20, 20, 20, 30, 20, 10 };
    
    int size = sizeof(alphabet) / sizeof(alphabet[0]);
    
    HuffmanCalc(alphabet, freq, size);
    
    for (int i = 0; i < hfIndex; i++) {
        cout << huffCodes[i] << endl;
        
    }
    
    return 0;
}
