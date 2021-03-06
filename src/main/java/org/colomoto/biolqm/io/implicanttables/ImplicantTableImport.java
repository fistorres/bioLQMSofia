package org.colomoto.biolqm.io.implicanttables;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.colomoto.biolqm.LogicalModel;
import org.colomoto.biolqm.LogicalModelImpl;
import org.colomoto.biolqm.NodeInfo;
import org.colomoto.biolqm.io.BaseLoader;
import org.colomoto.biolqm.io.antlr.ITNETLexer;
import org.colomoto.biolqm.io.antlr.ITNETParser;
import org.colomoto.biolqm.io.antlr.ErrorListener;
import org.colomoto.mddlib.MDDManager;
import org.colomoto.mddlib.MDDManagerFactory;
import org.colomoto.mddlib.MDDVariableFactory;
import org.colomoto.mddlib.operators.MDDBaseOperators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ImplicantTableImport extends BaseLoader {

    @Override
    protected LogicalModel performTask() throws Exception {

        CharStream input = new ANTLRInputStream(streams.reader());
        CommonTokenStream tokens = new CommonTokenStream( new ITNETLexer(input) );
        ITNETParser parser = new ITNETParser( tokens );
        ErrorListener errors = new ErrorListener();
        parser.addErrorListener(errors);
        ITNETParser.ModelContext mtx = parser.model();

        // Peak forward to create all components and prepare the MDD manager
        List<ITNETParser.TableContext> tables = mtx.table();
        int nbvar = tables.size();
        List<NodeInfo> components = new ArrayList<>(nbvar);
        Map<String,Integer> id2index = new HashMap<>();
        MDDVariableFactory mvf = new MDDVariableFactory();
        int max = 5;
        for (int idx=0 ; idx<nbvar ; idx++) {
            ITNETParser.TableContext ttx = tables.get(idx);
            String id = ttx.curvar().getText();
            id2index.put(id, idx);
            byte curmax = 1;
            try {
                curmax = Byte.parseByte( ttx.max().getText() );
            } catch (Exception e) {
                // no max was defined
            }
            NodeInfo ni = new NodeInfo(id, curmax);
            components.add(ni);
            if (curmax > max) {
                max = curmax;
            }
            mvf.add(ni, (byte)(curmax+1));
        }
        MDDManager ddmanager = MDDManagerFactory.getManager(mvf, max+1);


        // Load all functions
        int[] functions = new int[nbvar];
        for (ITNETParser.TableContext ttx: mtx.table()) {
            int curComponent = id2index.get( ttx.curvar().getText() );
            List<ITNETParser.VarContext> regulatorIDs = ttx.var();
            int[] regulators = new int[regulatorIDs.size()];
            int i = 0;
            for (ITNETParser.VarContext vtx: regulatorIDs) {
                regulators[i++] = id2index.get(vtx.getText());
            }

            List<ITNETParser.LineContext> ttlines = ttx.line();
            int count = regulators.length;
            int f = 0;
            for (ITNETParser.LineContext ltx: ttlines) {
                int targetValue = Integer.parseInt( ltx.target().getText() );
                if (targetValue == 0) {
                    continue;
                }

                String line = ltx.ttline().getText().trim();
                if (line.length() != count) {
                    throw new RuntimeException("Wrong number of values in implicant table line: "+line.length() +" expected "+ count);
                }

                byte[] vals = new byte[count];
                for (int idx=0 ; idx<count ; idx++) {
                    vals[idx] = (byte)Character.getNumericValue(line.charAt(idx));
                }

                byte[] state = new byte[nbvar];
                for (int k=0 ; k<state.length ; k++) {
                    state[k] = -1;
                }
                for (int k=0 ; k<regulators.length ; k++) {
                    state[ regulators[k] ] = vals[k];
                }

                // Extend the logical function
                // Here we do a simple OR as implicants should not overlap
                int newNode = ddmanager.nodeFromState(state, targetValue);
                int nextNode = MDDBaseOperators.OR.combine(ddmanager, f, newNode);
                ddmanager.free(newNode);
                ddmanager.free(f);
                f = nextNode;
            }

            // Save the full function
            functions[curComponent] = f;
        }

        // Assemble the complete model
        return new LogicalModelImpl(components, ddmanager, functions);
    }

}
