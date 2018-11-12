// function to convert decimal to binary
void decToBinary(int n)
{
    // array to store binary number
    int binaryNum[1000];
    int * a;
    int * b;
    // counter for binary array
    int i;
    int j;
    binaryNum[0] =10;
    binaryNum[1] =100;
    a= binaryNum;
    b = a;
    print_i(*b);
    i = 0;
    while (n > 0) {

        // storing remainder in binary array
        binaryNum[i] = n % 2;
        n = n / 2;
        i = i+1;
    }

    j = i-1;
    while (j>=0){
    //print_i(binaryNum[j]);
    j = j-1;
    }
}


// Driver program to test above function
int main()
{
    int n;
    n = 32;
    decToBinary(n);
    return 0;
}