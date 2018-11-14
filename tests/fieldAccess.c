struct x{
int y;
char z;
};

struct x c;

int plus (int a){
return a+3;
}
int main(){
struct x a;
int b;
a.z = 'j';
a.y = 7;
b = plus(a.y);
print_i(a.y);
print_i(b);
return plus(a.y);
}