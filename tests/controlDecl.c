int plus(int a){
int b;
b = 7;
while (b<8){
b = 9;
return b;}

return a+3;
}

int main(){
int a;
int b;
a = read_i();
if (a == 5){
int c;
int d;
c = 6;
print_i(c);
return c;
}
b = plus(a);
a = 5;
print_i(b);
return b;
}