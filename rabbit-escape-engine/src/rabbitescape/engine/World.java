package rabbitescape.engine;

import static rabbitescape.engine.util.Util.*;
import static rabbitescape.engine.Block.Type.*;

import java.util.List;
import java.util.Map;

import rabbitescape.engine.err.RabbitEscapeException;
import rabbitescape.engine.util.Dimension;

public class World
{
    public static class DontStepAfterFinish extends RabbitEscapeException
    {
        private static final long serialVersionUID = 1L;

        public final String worldName;

        public DontStepAfterFinish( String worldName )
        {
            this.worldName = worldName;
        }
    }

    public static class NoBlockFound extends RabbitEscapeException
    {
        private static final long serialVersionUID = 1L;

        public final int x;
        public final int y;

        public NoBlockFound( int x, int y )
        {
            this.x = x;
            this.y = y;
        }
    }

    public static class UnableToAddToken extends RabbitEscapeException
    {
        private static final long serialVersionUID = 1L;

        public final Token.Type ability;

        public UnableToAddToken( Token.Type ability )
        {
            this.ability = ability;
        }
    }

    public static class NoSuchAbilityInThisWorld extends UnableToAddToken
    {
        private static final long serialVersionUID = 1L;

        public NoSuchAbilityInThisWorld( Token.Type ability )
        {
            super( ability );
        }
    }

    public static class NoneOfThisAbilityLeft extends UnableToAddToken
    {
        private static final long serialVersionUID = 1L;

        public NoneOfThisAbilityLeft( Token.Type ability )
        {
            super( ability );
        }
    }

    public final Dimension size;
    public final List<Block> blocks;
    public final List<Rabbit> rabbits;
    public final List<Thing> things;
    public final Map<Token.Type, Integer> abilities;
    public final String name;
    public final int num_rabbits;
    public final int num_to_save;
    public final int rabbit_delay;

    public int num_saved;
    public int num_killed;
    public int num_waiting;

    public final WorldChanges changes;

    public World(
        Dimension size,
        List<Block> blocks,
        List<Rabbit> rabbits,
        List<Thing> things,
        Map<Token.Type, Integer> abilities,
        String name,
        int num_rabbits,
        int num_to_save,
        int rabbit_delay,
        int num_saved,
        int num_killed,
        int num_waiting
    )
    {
        this.size = size;
        this.blocks = blocks;
        this.rabbits = rabbits;
        this.things = things;
        this.abilities = abilities;
        this.name = name;
        this.num_rabbits = num_rabbits;
        this.num_to_save = num_to_save;
        this.rabbit_delay = rabbit_delay;
        this.num_saved = num_saved;
        this.num_killed = num_killed;
        this.num_waiting = num_waiting;

        this.changes = new WorldChanges( this );

        init();
    }

    private void init()
    {
        for ( Thing thing : allThings() )
        {
            thing.calcNewState( this );
        }
    }

    public void step()
    {
        if ( finished() )
        {
            throw new DontStepAfterFinish( name );
        }

        for ( Thing thing : allThings() )
        {
            thing.step(this);
        }

        changes.apply();

        for ( Thing thing : allThings() )
        {
            thing.calcNewState( this );
        }

        changes.apply();
    }

    public ChangeDescription describeChanges()
    {
        ChangeDescription ret = new ChangeDescription();

        for ( Thing thing : allThings() )
        {
            ret.add( thing.x, thing.y, thing.state );
        }

        return ret;
    }

    private Iterable<Thing> allThings()
    {
        return chain( rabbits, things );
    }

    public boolean flatBlockAt( int x, int y )
    {
        Block block = getBlockAt( x, y );
        return ( block != null && block.type == solid_flat );
    }

    public boolean slopingBlockAt( int x, int y )
    {
        Block block = getBlockAt( x, y );
        return ( block != null && block.type != solid_flat );
    }

    public Block getBlockAt( int x, int y )
    {
        // TODO: faster
        for ( Block block : blocks )
        {
            if ( block.x == x && block.y == y )
            {
                return block;
            }
        }
        return null;
    }

    public boolean finished()
    {
        return ( rabbits.size() == 0 && this.num_waiting <= 0 );
    }

    public Token getTokenAt( int x, int y )
    {
        // TODO: faster
        for ( Thing thing : things )
        {
            if ( thing.x == x && thing.y == y && thing instanceof Token )
            {
                return (Token)thing;
            }
        }
        return null;
    }

    public boolean success()
    {
        return num_saved > num_to_save;
    }

    public int numRabbitsOut()
    {
        return num_rabbits -
            ( this.num_waiting + num_killed + num_saved );
    }
}
