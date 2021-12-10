package methodFinder;

import java.util.ArrayList;
import java.util.List;

public class RepoStats {
    private final String name;
    private List<Long> chains;
    private List<Long> test_chains;

    public RepoStats(final String name)
    {
        this.name = name;
        chains = new ArrayList<>();
        test_chains = new ArrayList<>();
    }

    public void addObservation(int chain_length)
    {
        while(chains.size() <= chain_length)
            chains.add(0L);

        chains.set(chain_length, chains.get(chain_length) + 1L);
    }

    public void addTestObservation(int chain_length)
    {
        while(test_chains.size() <= chain_length)
            test_chains.add(0L);

        test_chains.set(chain_length, test_chains.get(chain_length) + 1L);
    }

    public String getObservations()
    {
        final StringBuilder result = new StringBuilder();
        result.append(name).append(",NT,").append(chains.size() - 1);

        pruneDuplicates();

        for(int i = 0; i < chains.size(); i++)
        {
            result.append(",");
            result.append(chains.get(i));
        }

        result.append("\n");

        result.append(name).append(",T,").append(test_chains.size() - 1);

        for(int i = 0; i < test_chains.size(); i++)
        {
            result.append(",");
            result.append(test_chains.get(i));
        }

        result.append("\n");

        return result.toString();
    }

    //Remove duplicates, stopping at 1, since length 0 is when an error occurs, and is not part of a chain.
    public void pruneDuplicates()
    {
        if(chains.size() >= 3) {
            long prev = chains.get(chains.size() - 1);
            for (int i = chains.size() - 2; i >= 1; i--) {
                chains.set(i, chains.get(i) - prev);
                prev = chains.get(i);
            }
        }

        if(test_chains.size() >= 3) {
            long prev = test_chains.get(test_chains.size() - 1);
            for (int i = test_chains.size() - 2; i >= 1; i--) {
                test_chains.set(i, test_chains.get(i) - prev);
                prev = test_chains.get(i);
            }
        }
    }

    public int longestChain()
    {
        return chains.size() - 1;
    }
}
