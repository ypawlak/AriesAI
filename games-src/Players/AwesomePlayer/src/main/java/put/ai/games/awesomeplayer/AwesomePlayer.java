/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package put.ai.games.awesomeplayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;

public class AwesomePlayer extends Player {

    private Random random = new Random(0xdeadbeef);

    Board inputBoard;

    private final Map<Color, BoardField> homeFields = new HashMap<>();

    private BoardField getHomeField(Color playerClr) {
        if (!homeFields.containsKey(playerClr)) {
            homeFields.put(playerClr, findHomeField(playerClr));
        }
        return homeFields.get(playerClr);
    }

    private BoardField getGoalField(Color playerClr) {
        Color opponentClr = getOpponent(playerClr);
        if (!homeFields.containsKey(opponentClr)) {
            homeFields.put(opponentClr, findHomeField(opponentClr));
        }
        return homeFields.get(opponentClr);
    }
    
    private BoardField findHomeField(Color playerClr) {
        int boardSize = inputBoard.getSize();
        if (inputBoard.getState(0, 0) == playerClr) {
            return new BoardField(0, 0);
        } else {
            return new BoardField(boardSize - 1, boardSize - 1);
        }
    }

    @Override
    public String getName() {
        return "Jan Pawlak 117241 Piotr Markowski 117204";
    }

    @Override
    public Move nextMove(Board b) {
        inputBoard = b;
        return getMoveByMaxMinStrategy(getColor(), b, 1, OPTIMIZATION_TYPE.MAX).getAction();
    }

    private HeuristicMove getMoveByMaxMinStrategy(Color currentPlayer, Board board,
            int depth, OPTIMIZATION_TYPE optDirection) {
        if (isGameOver(board) || depth == 0) {
            return getHeuristicallyBestMove(currentPlayer, board);
        }
        List<Move> moves = board.getMovesFor(currentPlayer);
        HeuristicMove best;

        if (optDirection == OPTIMIZATION_TYPE.MAX) {
            best = new HeuristicMove(null, 0, OPTIMIZATION_TYPE.MAX);
            for (Move move : moves) {
                board.doMove(move);
                HeuristicMove trial = getMoveByMaxMinStrategy(
                        getOpponent(currentPlayer), board, depth - 1, OPTIMIZATION_TYPE.MIN);
                board.undoMove(move);

                if (trial.isGreaterThan(best)) {
                    best = new HeuristicMove(move, trial.getEvaluation(),
                            trial.getEvaluationDirection());
                }
            }
        } else {
            best = new HeuristicMove(null, board.getSize(), OPTIMIZATION_TYPE.MIN);
            for (Move move : moves) {
                board.doMove(move);
                HeuristicMove trial = getMoveByMaxMinStrategy(
                        getOpponent(currentPlayer), board, depth - 1, OPTIMIZATION_TYPE.MAX);
                board.undoMove(move);

                if (trial.isLessThan(best)) {
                    best = new HeuristicMove(move, trial.getEvaluation(),
                            trial.getEvaluationDirection());
                }
            }
        }

        return best;
    }

    private boolean isGameOver(Board board) {
        return hasWon(getColor(), board)
                || hasWon(getOpponent(getColor()), board);
    }

    private boolean hasWon(Color player, Board board) {
        BoardField goal = getGoalField(player);
        Color goalFieldColor = board.getState(goal.getX(), goal.getY());
        return goalFieldColor.equals(player);
    }

    private HeuristicMove getHeuristicallyBestMove(Color currentPlayer, Board board) {
        BoardField home = getHomeField(currentPlayer);
        BoardField goal = getGoalField(currentPlayer);

        BoardField myBestPawn = getBestPawn(currentPlayer, board);
        int goalDistance = getChebyshevDistance(myBestPawn, goal);
        BoardField opponentsBestPawn = getBestPawn(getOpponent(currentPlayer), board);
        int oponentDistance = getChebyshevDistance(opponentsBestPawn, home);

        boolean mustDefend = oponentDistance < board.getSize() / 2;

        List<Move> moves = board.getMovesFor(currentPlayer);
        Move bestMove = null;

        int heuristicEvaluation = goalDistance;
        int oponentEvaluation = oponentDistance;

        for (Move move : moves) {
            board.doMove(move);

            if (mustDefend) {
                BoardField oponentsNewBest = getBestPawn(getOpponent(currentPlayer), board);
                int newOponentDist = getChebyshevDistance(oponentsNewBest, home);
                if (newOponentDist > oponentEvaluation) {
                    oponentEvaluation = newOponentDist;
                    bestMove = move;
                }
            } else {
                BoardField newBest = getBestPawn(currentPlayer, board);
                int newDist = getChebyshevDistance(newBest, goal);
                if (newDist < heuristicEvaluation) {
                    heuristicEvaluation = newDist;
                    bestMove = move;
                }
            }

            board.undoMove(move);
        }
        if (bestMove == null) {
            //hack PM & JP just in case
            bestMove = moves.get(random.nextInt(moves.size()));
        }

        OPTIMIZATION_TYPE evalOptDirection;
        int eval;

        if (mustDefend) {
            evalOptDirection = OPTIMIZATION_TYPE.MAX;
            eval = oponentEvaluation;
        } else {
            evalOptDirection = OPTIMIZATION_TYPE.MIN;
            eval = heuristicEvaluation;
        }

        return new HeuristicMove(bestMove, eval, evalOptDirection);
    }

    private BoardField getBestPawn(Color playerClr, Board board) {
        int boardSize = board.getSize();
        BoardField goalField = getGoalField(playerClr);
        BoardField bestPawn = null;
        for (int x = 0; x < boardSize; x++) {
            for (int y = 0; y < boardSize; y++) {
                Color fieldColor = board.getState(x, y);
                if (fieldColor == playerClr) {
                    BoardField currentField = new BoardField(x, y);
                    if (bestPawn == null
                            || getChebyshevDistance(currentField, goalField) < getChebyshevDistance(bestPawn, goalField)) {
                        bestPawn = currentField;
                    }
                }
            }
        }
        return bestPawn;
    }

    private int getChebyshevDistance(BoardField fieldA, BoardField fieldB) {
        int distance = Math.abs(fieldA.getX() - fieldB.getX())
                + Math.abs(fieldA.getY() - fieldB.getY());
        return distance;
    }
}

class BoardField {

    private int x;
    private int y;

    public BoardField(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}

class HeuristicMove implements Comparable<HeuristicMove> {

    private final Move action;
    private final Integer evaluation;
    private final OPTIMIZATION_TYPE evaluationDirection;

    public HeuristicMove(Move action, int eval, OPTIMIZATION_TYPE direction) {
        this.action = action;
        this.evaluation = eval;
        this.evaluationDirection = direction;
    }

    @Override
    public int compareTo(HeuristicMove o) {
        if (evaluationDirection == o.evaluationDirection) {
            return evaluationDirection == OPTIMIZATION_TYPE.MIN
                    ? o.evaluation - evaluation
                    : evaluation - o.evaluation;
        }

        //HACK JP założone apriori - jeśli nie musimy się bronić jesteśmy lepsi niż jeśli musimy
        return evaluationDirection == OPTIMIZATION_TYPE.MIN
                ? 1
                : -1;
    }

    public boolean isGreaterThan(HeuristicMove that) {
        return this.compareTo(that) > 0;
    }

    public boolean isLessThan(HeuristicMove that) {
        return this.compareTo(that) < 0;
    }

    public Move getAction() {
        return action;
    }

    public Integer getEvaluation() {
        return evaluation;
    }

    public OPTIMIZATION_TYPE getEvaluationDirection() {
        return evaluationDirection;
    }
}

enum OPTIMIZATION_TYPE {

    MAX,
    MIN
}
