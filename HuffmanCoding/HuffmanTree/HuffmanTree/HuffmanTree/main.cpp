//
//  main.cpp
//  HuffmanTree
//
//  Created by Elias Piovani on 12/01/2018.
//  Copyright © 2018 Elias Piovani. All rights reserved.
//

#include <iostream>
#include <list>

struct node{
    char data;
    unsigned freq;
    node *left, *right;
    
};

struct node* newNode(char data, int freq)
{
    node* temp = new node;
    temp->data = data;
    temp->freq = freq;
    
    return temp;
};

//void addNode (node* )
//{
//    parent->data =
//}

void huffmanTree(char data[], int freq[], int size)
{
    int fMin = INT_MAX, sMin = INT_MAX;
    int i_ = NULL;
    char fMinC = '\0', sMinC = '\0';
    node* root;
    
    for (int i = 0; i < size; i++) {
        if (freq[i] < fMin) {
            sMin = fMin;
            sMinC = data[i_];
            fMin = freq[i];
            fMinC = data[i];
            i_ = i;
        }
        else if (freq[i] < sMin) {
            sMin = freq[i];
            sMinC = data[i];
        }
    }
    
    std::cout << fMin << "-" << fMinC << " " << sMin << "-" << sMinC;
    //find the two smallest numbers
    
    //add these node for these numbers
    root = newNode(NULL, fMin + sMin);
    root->left = newNode(fMinC, fMin);
    root->right = newNode(sMinC, sMin);
    
}

int main(int argc, const char * argv[]) {
    char alphabet[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    int frequency[] = {1241, 1608, 415, 343, 288, 272, 158, 111, 180, 151};
    int size = sizeof(frequency) / sizeof(frequency[0]);
    
    huffmanTree(alphabet, frequency, size);
}
