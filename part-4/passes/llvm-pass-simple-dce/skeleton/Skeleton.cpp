#define DEBUG_TYPE "SimpleDCE"

#include "llvm/Pass.h"
#include "llvm/IR/Function.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/IR/LegacyPassManager.h"
#include "llvm/Transforms/IPO/PassManagerBuilder.h"
#include "llvm/Transforms/Utils/Local.h"
#include <map>
#include <vector>

using namespace llvm;
using namespace std;


namespace {
    struct SimpleDCE : public FunctionPass {
        static char ID;
        SmallVector<Instruction *, 64> Worklist;
        SimpleDCE() : FunctionPass(ID) {}

        virtual bool runOnFunction(Function &F) {
            bool changed = true;
            bool cutInstruction = false;
            while (changed) {
                changed = false;
                for (Function::iterator bb = F.begin(), e = F.end(); bb != e; ++bb) {
                    for (BasicBlock::iterator i = bb->begin(), e = bb->end(); i != e; ++i) {
                        Instruction* inst = &*i;
                        if (isInstructionTriviallyDead(inst)) {
                            changed = true;
                            cutInstruction = true;
                            Worklist.push_back(inst);
                        }
                    }
                }
                while (!Worklist.empty()){
                    Instruction* inst = Worklist.pop_back_val();
                    inst->eraseFromParent();
                }
            }

            return cutInstruction;
        }
    };
}

char SimpleDCE::ID = 0;
static RegisterPass<SimpleDCE> X("skeletonpass", "Simple dead code elimination");
/*// Automatically enable the pass.
// http://adriansampson.net/blog/clangpass.html
static void registerSkeletonPass(const PassManagerBuilder &,
                                 legacy::PassManagerBase &PM) {
    PM.add(new CountOp());
}

static RegisterStandardPasses
        RegisterMyPass(PassManagerBuilder::EP_EarlyAsPossible,
                       registerSkeletonPass);*/