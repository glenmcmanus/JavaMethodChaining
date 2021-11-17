package methodFinder;

import java.util.ArrayList;
import java.util.List;

public class RepoStats {
    private final String name;
    private List<Long> chains;

    public RepoStats(final String name)
    {
        this.name = name;
        chains = new ArrayList<>();
    }

    public void addObservation(int chain_length)
    {
        while(chains.size() <= chain_length)
            chains.add(0L);

        chains.set(chain_length, chains.get(chain_length) + 1L);
    }

    public String getObservations()
    {
        final StringBuilder result = new StringBuilder();
        result.append(name).append(",").append(chains.size() - 1);

        if(chains.size() >= 3)
            pruneDuplicates();

        for(int i = 0; i < chains.size(); i++)
        {
            result.append(",");
            result.append(chains.get(i));
        }

        result.append("\n");

        return result.toString();
    }

    //Remove duplicates, stopping at 1, since length 0 is when an error occurs, and is not part of a chain.
    public void pruneDuplicates()
    {
        long prev = chains.get(chains.size() - 1);
        for(int i = chains.size() - 2; i >= 1; i--)
        {
            chains.set(i, chains.get(i) - prev);
            prev = chains.get(i);
        }
    }

    public int longestChain()
    {
        return chains.size() - 1;
    }
}
