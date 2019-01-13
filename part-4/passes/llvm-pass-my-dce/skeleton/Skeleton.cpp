#define DEBUG_TYPE "SimpleDCE"

#include "llvm/Pass.h"
#include "llvm/IR/Function.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/IR/LegacyPassManager.h"
#include "llvm/Transforms/IPO/PassManagerBuilder.h"
#include "llvm/Transforms/Utils/Local.h"
#include "llvm/ADT/BitVector.h"
#include <map>
#include <vector>

using namespace llvm;
using namespace std;

typedef std::map<Value *, std::set<Value *>> InstValueSetMap;
bool printed = false;
InstValueSetMap computeLiveness(Function &F) {
    // Initialize sets
    typedef std::map<Value *, std::map<BasicBlock*, std::set<Value *>>> PhiBlockValueSetMap;
    typedef std::set<Value *> ValueSet;
    // Initialize maps between instructions and sets for live-in and live-out
    PhiBlockValueSetMap phis;
    InstValueSetMap out;
    InstValueSetMap in;
    InstValueSetMap diff;
    InstValueSetMap outPrev;
    InstValueSetMap inPrev;
    ValueSet empty;
    // Iterate over basic blocks
    for (Function::iterator bb = F.begin(), e = F.end(); bb != e; ++bb) {
        // Iterate over instructions
        for (BasicBlock::iterator i = bb->begin(), e = bb->end(); i != e; ++i) {
            Instruction *inst = &*i;
            // in[n] = 0; out[n] = 0
            out[inst] = empty;
            in[inst] = empty;
            diff[inst] = empty;
            outPrev[inst] = empty;
            inPrev[inst] = empty;
        }
    }
    bool changed;
    do {
        for (Function::iterator bb = F.begin(), e = F.end(); bb != e; ++bb) {
            // Iterate over instructions
            for (BasicBlock::iterator i = bb->begin(), e = bb->end(); i != e; ++i) {
                Instruction *inst = &*i;
                // in'[n] = in[n]
                inPrev[inst] = in[inst];
                // out'[n] = out[n]
                outPrev[inst] = out[inst];
                // Sets for use[n] def[n]
                ValueSet use;
                ValueSet def;
                // Set the def set to be the instruction itself
                // def[n]
                //errs() << *inst << " Added to def set" << '\n';
                def.insert(inst);

                // If our instruction is a PHI instruction, the use set depends on the basic block
                // we came from, and need multiple use sets
                if (PHINode *PHIinst = dyn_cast<PHINode>(inst)){
                    //errs() << *PHIinst << '\n';
                    // Get number of possible values of instruction
                    int numArgs = PHIinst->getNumIncomingValues();
                    // For each incoming value figure out if it's an instruction/argument or don't add it
                    for (int ind = 0; ind < numArgs; ind++) {
                        Value* val = PHIinst->getIncomingValue(ind);
                        if (isa<Instruction>(val) || isa<Argument>(val)) {
                            BasicBlock* valBB = PHIinst->getIncomingBlock(ind);
                            // Create use set for each basic block
                            phis[PHIinst][valBB].insert(val);
                        }
                    }
                    // If it's not a PHI instruction simply iterate over instruction and
                    // get operands and store them into the use set
                } else {
                    for (User::op_iterator i = inst->op_begin(); i != inst->op_end(); i++) {
                        if (isa<Instruction>(&*i) || isa<Argument>(&*i)) {
                            // Get operands
                            Value * op = dyn_cast<Value>(&*i);
                            // Set the use set to be the operands of the instruction
                            // use[n]
                            use.insert(op);
                        }
                    }
                }
                // out[n] - def[n]
                std::set_difference(out[inst].begin(), out[inst].end(),
                                    def.begin(), def.end(),
                                    std::inserter (diff[inst], diff[inst].begin()));
                //while (!diff.empty()) {
                //    errs() << diff.pop_back_val() << " set difference between out[n] and def[n] \n";
                //}

                in[inst].clear();
                // in[n] = use[n] U (out[n]-def[n])
                std::set_union(use.begin(),use.end(),
                               diff[inst].begin(), diff[inst].end(),
                               std::inserter(in[inst],in[inst].begin()));
                //while (!in[inst].empty()) {
                //    in[inst].pop_back_val()->printAsOperand(errs(),false);
                //    errs()  << " live-in set of instruction " << *inst << '\n';
                //}

                // Get all successor instructions
                ValueSet succInsts;
                ValueSet brokenPHIs;
                if (inst->isTerminator()) {
                    for (int i = 0; i < inst->getNumSuccessors(); i++){
                        BasicBlock* succBB = inst->getSuccessor(i);
                        auto it = succBB->begin();
                        auto succInst = &*it;
                        succInsts.insert(succInst);
//                        if (isa<PHINode>(succInst)) {
//                            for (it++; it != succBB->end(); it++) {
//                                if (!isa<PHINode>(&*it)) {
//                                    break;
//                                }
//                                brokenPHIs.insert(&*it);
//                            }
//                        }
                    }
                } else {
                    auto iterCopy = i;
                    ++iterCopy;
                    Instruction* succInst = &*iterCopy;
                    succInsts.insert(succInst);
                }

                // Perform set operations
                ValueSet outTemp1 = empty;
                for (Value* succInst:succInsts) {
                    ValueSet outTemp2 = empty;
                    // Set out set to be equal to union of all in sets of successor nodes
                    if (PHINode * PHIinst = dyn_cast<PHINode>(succInst)){
                        if (inst->isTerminator()){
                            // create new in set for PHInode
                            ValueSet inTemp1 = empty;
                            std::set_union(phis[PHIinst][&*bb].begin(),phis[PHIinst][&*bb].end(),
                                           diff[PHIinst].begin(), diff[PHIinst].end(),
                                           std::inserter(inTemp1,inTemp1.begin()));
                            // out[n] = U in[s], s == succ[n]
                            std::set_union(outTemp1.begin(),outTemp1.end(),
                                           inTemp1.begin(), inTemp1.end(),
                                           std::inserter(outTemp2, outTemp2.begin()));
                            outTemp1 = outTemp2;
                        } else {
                            // create new in set for PHInode
                            ValueSet inTemp1 = empty;
                            ValueSet inTemp2 = empty;
                            std::set_union(inTemp2.begin(),inTemp2.end(),
                                           diff[PHIinst].begin(), diff[PHIinst].end(),
                                           std::inserter(inTemp1,inTemp1.begin()));
                            // out[n] = U in[s], s == succ[n]
                            std::set_union(outTemp1.begin(),outTemp1.end(),
                                           inTemp1.begin(), inTemp1.end(),
                                           std::inserter(outTemp2, outTemp2.begin()));
                            outTemp1 = outTemp2;
                        }
                    } else {
                        std::set_union(outTemp1.begin(),outTemp1.end(),
                                       in[succInst].begin(), in[succInst].end(),
                                       std::inserter(outTemp2, outTemp2.begin()));
                        outTemp1 = outTemp2;
                    }
                }
                out[inst] = outTemp1;
                //while (!out[inst].empty()) {
                //    out[inst].pop_back_val()->printAsOperand(errs(),false);
                //    errs()  << " live-out set of instruction " << *inst << '\n';
                //}
            }
        }

        // Check for convergence
        changed = false;
        for (Function::iterator bb = F.begin(), e = F.end(); bb != e; ++bb) {
            // Iterate over instructions
            for (BasicBlock::iterator i = bb->begin(), e = bb->end(); i != e; ++i) {
                Instruction *inst = &*i;
                // until in'[n]=in[n] and out'[n]=out[n] for all n
                if (inPrev[inst] != in[inst] || outPrev[inst] != out[inst]){
                    changed = true;
                }
            }
        }
//        //PRINT LIVENESS
//        for (Function::iterator bb = F.begin(), e = F.end(); bb != e; ++bb) {
//            // Iterate over instructions
//            for (BasicBlock::iterator i = bb->begin(), e = bb->end(); i != e; ++i) {
//                Instruction *inst = &*i;
//                errs() << "live-in: {";
//                for (Value* instruction : in[inst]){
//                    instruction->printAsOperand(errs(),false);
//                    errs()  << " ";
//                }
//                errs() << "} \n";
//                errs() << *inst << '\n';
//                errs() << "live-out: {";
//                for (Value* instruction : out[inst]){
//                    instruction->printAsOperand(errs(),false);
//                    errs()  << " ";
//                }
//                errs() << "} \n";
//            }
//        }

    } while (changed);
    if (printed == false){
        for (BasicBlock &bb : F) {
            for (auto iter = bb.begin(); iter != bb.end(); ++iter) {
                Instruction* I = &*iter;
                // Ignore phinodes
                if (isa<PHINode>(I)) {
                    continue;
                }
                errs() << "{";
                int i=0;
                for (auto V : in[I]) {

                    V->printAsOperand(errs(), false);
                    i++;
                    if (i < in[I].size()) {
                        errs() << ",";
                    }
                }
                errs() << "}\n";
            }
        }
        errs() << "{}\n";
    }

    return out;
}

namespace {
    struct MyDCE : public FunctionPass {
        static char ID;
        SmallVector<Instruction *, 64> Worklist;
        MyDCE() : FunctionPass(ID) {}

        virtual bool runOnFunction(Function &F) {
            bool cutInstruction = false;
            bool changed = true;
            printed = false;
            while (changed) {
                InstValueSetMap out;
                out = computeLiveness(F);
                printed = true;
                changed = false;
                for (Function::iterator bb = F.begin(), e = F.end(); bb != e; ++bb) {
                    for (BasicBlock::iterator i = bb->begin(), e = bb->end(); i != e; ++i) {
                        Instruction *inst = &*i;
                        if (inst->isSafeToRemove()){
                            if (out[inst].find(inst) == out[inst].end()){
                                changed = true;
                                cutInstruction = true;
                                Worklist.push_back(inst);
                            }
                        }
                    }
                }
                while (!Worklist.empty()) {
                    Instruction *inst = Worklist.pop_back_val();
                    inst->eraseFromParent();
                }
            }
            return cutInstruction;
        }
    };
}

char MyDCE::ID = 0;
static RegisterPass <MyDCE> X("live", "My own dead code elimination");
/*// Automatically enable the pass.
// http://adriansampson.net/blog/clangpass.html
static void registerSkeletonPass(const PassManagerBuilder &,
                                 legacy::PassManagerBase &PM) {
    PM.add(new CountOp());
}

static RegisterStandardPasses
        RegisterMyPass(PassManagerBuilder::EP_EarlyAsPossible,
                       registerSkeletonPass);*/