/*
 * SinglePassRegPruner.java
 * (this file is part of MYRA)
 * 
 * Copyright 2008-2018 Fernando Esteban Barril Otero
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package myra.regression;

import static myra.Config.CONFIG;
import static myra.datamining.Dataset.COVERED;
import static myra.datamining.Dataset.RULE_COVERED;
import static myra.datamining.IntervalBuilder.MINIMUM_CASES;
import static myra.rule.Assignator.ASSIGNATOR;

import myra.Cost;
import myra.regression.rule.RegressionRule;
import myra.datamining.Dataset;
import myra.datamining.Attribute.Condition;
import myra.datamining.Dataset.Instance;
import myra.rule.Assignator;
import myra.rule.Pruner;
import myra.rule.Rule;
import myra.rule.RuleFunction;
import myra.rule.Rule.Term;

/**
 * @author amh58
 */
public class SinglePassRegPruner extends Pruner {
    @Override
    public int prune(Dataset dataset,
		     Rule rule,
		     Instance[] instances,
		     RuleFunction function) {
	Term[] terms = rule.terms();
	
	Coverage[] coverage = new Coverage[rule.terms().length];
	
	for(int i = 0; i < coverage.length; i++)
	{
	    coverage[i] = new Coverage(instances.length);
	}
	
	// (1) determines the coverage of each term

	for (int i = 0; i < dataset.size(); i++) {
	    // only considers instances not covered
	    if (instances[i].flag != COVERED) {

		for (int j = 0; j < terms.length; j++) {
		    if (terms[j].isEnabeld()) {
			Condition condition = terms[j].condition();
			double v = dataset.value(i, condition.attribute);

			if (condition.satisfies(v)) {
			    coverage[j].covered[i].flag = RULE_COVERED;
			}
			else
			{
			    break;
			}
		    }
		}
	    }
	}

	// (2) determines the quality of each term

	Assignator assignator = CONFIG.get(ASSIGNATOR);

	
	int selected = -1;
	Cost best = null;

	for (int i = 0; i < coverage.length; i++) {
	    // the rule must cover a minimum number of cases, therefore
	    // only terms that cover more than the limit are considered
	    if (coverage[i].total() >= CONFIG.get(MINIMUM_CASES)) {
		
		
		assignator.assign(dataset, rule, coverage[i].covered);

		Cost current = function.evaluate(dataset, rule, coverage[i].covered);

		if (best == null || current.compareTo(best) >= 0) {
		    selected = i;
		    best = current;
		}
	    } else {
		// stops the procedure, since any remaining term will not
		// cover the minimum number
		break;
	    }
	}

	// (3) disables and removes unused terms

	for (int i = selected + 1; i < terms.length; i++) {
	    terms[i].setEnabeld(false);
	}

	rule.setQuality(best);
	rule.compact();
	rule.apply(dataset, instances);

	return assignator.assign(dataset, rule, instances);
    }
    /**
     * Class to store the coverage information of a term.
     * 
     * @author Fernando Esteban Barril Otero
     */
    private static class Coverage {
	/**
	 * Covered instances information.
	 */
	Instance [] covered;

	
	/**
	 * Default constructor.
	 * 
	 * @param length
	 *            the number of classes.
	 */
	Coverage(int length) {
	    covered = new Instance[length];
	    for(int i=0 ; i < length ; i++)
	    {
		covered[i] = new Instance();
	    }
	}

	/**
	 * Returns the total number of covered examples.
	 * 
	 * @return the total number of covered examples.
	 */
	int total() {
	    int total = 0;

	    for (int i = 0; i < covered.length; i++) {
		if(covered[i].flag == RULE_COVERED)
		    total ++;
	    }

	    return total;
	}
    }
}