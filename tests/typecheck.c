struct a {
 void z;
 char y;
};
int ok;
char g[9];
// cannot declare a variable as void

void yes(int a, int b){
struct a kk;
void* ops;
int* r;
void op[12];
// function has different amount of arguments
yes(4);
// function has different types of arguments
yes('a',3);
// cannot add not an int
"lol"+3;
// cannot compare expr of type void
yes == ok;
// cannot compare expr of type struct
kk != ok;
// cannot access array at a non-int position
g["okay"];
g[g];
// cannot access not an array at a position
ok[1];
// void array should work

op[1];
// void pointer should work

ops[2];
// pointer to char

// this should work
r+6;
}
