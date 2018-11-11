int minus (int a ,int b){
return a-b;
}

int plus(int a, int b){
return minus(a,b)+minus(b,a)+minus(b,a)+minus(b,a);
}


void main(){
int a;
int b;
a = 5;
b = 6;
print_i(plus(a,b));
}