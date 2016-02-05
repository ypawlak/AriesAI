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
        return getMoveByMaxMinStrategy(getColor(), b, 2, OPTIMIZATION_TYPE.MAX).getAction();
    }

    private HeuristicMove getMoveByMaxMinStrategy(Color currentPlayer, Board board,
            int depth, OPTIMIZATION_TYPE optDirection) {

        if (depth == 0) {
            return getHeuristicallyBestMove(currentPlayer, board);
        }

        List<Move> moves = board.getMovesFor(currentPlayer);
        HeuristicMove bestMove;

        if (optDirection == OPTIMIZATION_TYPE.MAX) {
            //szukamy najlepszej

            //najsłabsza możliwa ocena heurystyczna
            bestMove = new HeuristicMove(null, 0, OPTIMIZATION_TYPE.MAX);
            for (Move move : moves) {
                HeuristicMove trial;
                board.doMove(move);
                try {
                    checkGameOver(currentPlayer, board);
                    trial = getMoveByMaxMinStrategy(getOpponent(currentPlayer),
                            board, depth - 1, OPTIMIZATION_TYPE.MIN);
                } catch (VictoryException ex) {
                    //najlepsza możliwa ocena heurystyczna, możemy skończyć szukanie
                    return new HeuristicMove(move, 0, OPTIMIZATION_TYPE.MIN);
                } catch (DefeatException ex) {
                    //najsłabsza możliwa ocena heurystyczna, chcemy lepiej
                    trial = new HeuristicMove(move, 0, OPTIMIZATION_TYPE.MAX);
                } finally {
                    board.undoMove(move);
                }

                if (trial.isGreaterThan(bestMove)) {
                    bestMove = new HeuristicMove(move, trial.getEvaluation(),
                            trial.getEvaluationDirection());
                }
            }
        } else {
            //szukamy najgorszej

            //najlepsza ocena heurystyczna
            bestMove = new HeuristicMove(null, 0, OPTIMIZATION_TYPE.MIN);
            for (Move move : moves) {
                HeuristicMove trial;
                board.doMove(move);
                try {
                    checkGameOver(currentPlayer, board);
                    trial = getMoveByMaxMinStrategy(getOpponent(currentPlayer),
                            board, depth - 1, OPTIMIZATION_TYPE.MAX);
                } catch (VictoryException ex) {
                    //najgorsza ocena heurystyczna, możemy skończyć szukanie
                    return new HeuristicMove(move, 0, OPTIMIZATION_TYPE.MAX);
                } catch (DefeatException ex) {
                    //najlepsza ocena heurystyczna, chcemy lepiej
                    trial = new HeuristicMove(move, 0, OPTIMIZATION_TYPE.MIN);
                } finally {
                    board.undoMove(move);
                }

                if (trial.isLessThan(bestMove)) {
                    bestMove = new HeuristicMove(move, trial.getEvaluation(),
                            trial.getEvaluationDirection());
                }
            }
        }
        
        return bestMove;
    }

    private void checkGameOver(Color currentPlayer, Board board) throws VictoryException, DefeatException {
        if (board.getWinner(currentPlayer) == currentPlayer) {
            throw new VictoryException();
        }
        Color oponent = getOpponent(currentPlayer);
        if (board.getWinner(oponent) == oponent) {
            throw new DefeatException();
        }
    }

    private HeuristicMove getHeuristicallyBestMove(Color currentPlayer, Board board) {
        BoardField home = getHomeField(currentPlayer);
        BoardField goal = getGoalField(currentPlayer);

        List<Move> moves = board.getMovesFor(currentPlayer);

        BoardField myBestPawn = getBestPawn(currentPlayer, board);

        int goalDistance = getDistanceToWin(board, myBestPawn, goal);

        BoardField opponentsBestPawn = getBestPawn(getOpponent(currentPlayer), board);

        int oponentDistance = getDistanceToWin(board, opponentsBestPawn, home);

        boolean mustDefend = oponentDistance < board.getSize() / 2;

        Move bestMove = null;

        int heuristicEvaluation = goalDistance;
        int oponentEvaluation = oponentDistance;

        for (Move move : moves) {
            board.doMove(move);

            if (mustDefend) {
                BoardField oponentsNewBest = getBestPawn(getOpponent(currentPlayer), board);
                int newOponentDist = getDistanceToWin(board, oponentsNewBest, home);
                if (newOponentDist > oponentEvaluation) {
                    oponentEvaluation = newOponentDist;
                    bestMove = move;
                }
            } else {
                BoardField newBest = getBestPawn(currentPlayer, board);
                int newDist = getDistanceToWin(board, newBest, goal);
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
                            || getDistanceToWin(board, currentField, goalField) < getDistanceToWin(board, bestPawn, goalField)) {
                        bestPawn = currentField;
                    }
                }
            }
        }
        return bestPawn;
    }

    private int getDistanceToWin(Board board, BoardField fieldA, BoardField fieldB) {
        int distance = Math.abs(fieldA.getX() - fieldB.getX())
                + Math.abs(fieldA.getY() - fieldB.getY());
        if (distance > 0 && checkFreeWay(board, fieldA, fieldB)) {
            distance = 1;
        }
        return distance;
    }

    private boolean checkFreeWay(Board board, BoardField fieldA, BoardField fieldB) {
        boolean isFree = true;
        switch (checkCollinearity(fieldA, fieldB)) {
            case X:
                if (fieldA.getY() < fieldB.getY()) {
                    for (int i = fieldA.getY() + 1; i < fieldB.getY(); i++) {
                        if (board.getState(fieldA.getX(), i) == getColor()
                                || board.getState(fieldA.getX(), i) == getOpponent(getColor())) {
                            isFree = false;
                        }
                    }
                } else {
                    for (int i = fieldB.getY() + 1; i < fieldA.getY(); i++) {
                        if (board.getState(fieldA.getX(), i) == getColor()
                                || board.getState(fieldA.getX(), i) == getOpponent(getColor())) {
                            isFree = false;
                        }
                    }
                }
                break;
            case Y:
                if (fieldA.getX() < fieldB.getX()) {
                    for (int i = fieldA.getX() + 1; i < fieldB.getX(); i++) {
                        if (board.getState(fieldA.getY(), i) == getColor()
                                || board.getState(i, fieldA.getY()) == getOpponent(getColor())) {
                            isFree = false;
                        }
                    }
                } else {
                    for (int i = fieldB.getX() + 1; i < fieldA.getX(); i++) {
                        if (board.getState(fieldA.getY(), i) == getColor()
                                || board.getState(i, fieldA.getY()) == getOpponent(getColor())) {
                            isFree = false;
                        }
                    }
                }
                break;
            default:
                isFree = false;
                break;
        }
        return isFree;
    }

    private COLLINEARITY checkCollinearity(BoardField fieldA, BoardField fieldB) {
        if (fieldA.getX() == fieldB.getX()) {
            return COLLINEARITY.X;
        } else if (fieldA.getY() == fieldB.getY()) {
            return COLLINEARITY.Y;
        }
        return COLLINEARITY.NONE;
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

        //HACK JP założenie apriori - jeśli nie musimy się bronić jesteśmy lepsi niż jeśli musimy
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

enum COLLINEARITY {

    X,
    Y,
    NONE
}

class VictoryException extends Exception {

}

class DefeatException extends Exception {

}
