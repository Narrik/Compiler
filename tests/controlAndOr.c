void main(){
char a;
a = 'l';
if (a == 'l' && 9 != 7){
print_s("AND_right");
} else {
print_i(0);}
if (a != 'l' && 9 != 7){
print_s("AND_wrong");
} else {
print_i(12);
}
if (a == 'l' || 9 == 7){
print_s("OR_right");
}
if (a == 'p' || 9 != 7){
print_s("OR_right");
}
if (a == 'k' || 9 == 7){
print_s("OR_wrong");
} else {
print_i(42);
}
}