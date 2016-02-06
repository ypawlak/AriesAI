/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package put.ai.games.pawncounter;

import java.util.List;
import java.util.Random;
import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;

public class PawnCounter extends Player {

    private Random random = new Random(0xdeadbeef);

    Board inputBoard;

    private Integer worstHeuristicEval;
    private Integer bestHeuristicEval;

    private int getWorstHeuristicEval(Board board) {
        if (worstHeuristicEval == null) {
            int boardSize = board.getSize();
            int pawnRowCount = boardSize / 2;
            worstHeuristicEval = pawnRowCount * pawnRowCount * (-1);
        }
        return worstHeuristicEval;
    }

    private int getBestHeuristicEval(Board board) {
        if (bestHeuristicEval == null) {
            int boardSize = board.getSize();
            int pawnRowCount = boardSize / 2;
            bestHeuristicEval = pawnRowCount * pawnRowCount;
        }
        return bestHeuristicEval;
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
            int heuristicVal = getPawnsCount(board);
            return new HeuristicMove(null, heuristicVal);
        }

        List<Move> moves = board.getMovesFor(currentPlayer);
        HeuristicMove bestMove;

        if (optDirection == OPTIMIZATION_TYPE.MAX) {
            bestMove = new HeuristicMove(null, getWorstHeuristicEval(board) - 1);
            for (Move move : moves) {
                HeuristicMove trial = null;
                board.doMove(move);

                switch (checkGameOver(currentPlayer, board)) {
                    case WON:
                        board.undoMove(move);
                        return new HeuristicMove(move, getBestHeuristicEval(board));
                    case LOST:
                        trial = new HeuristicMove(move, getWorstHeuristicEval(board));
                        break;
                    case UNSETTLED:
                        trial = getMoveByMaxMinStrategy(getOpponent(currentPlayer),
                                board, depth - 1, OPTIMIZATION_TYPE.MIN);

                }

                board.undoMove(move);
                if (trial.getEvaluation() > bestMove.getEvaluation()) {
                    bestMove = new HeuristicMove(move, trial.getEvaluation());
                }
            }
        } else {
            bestMove = new HeuristicMove(null, getBestHeuristicEval(board) + 1);
            for (Move move : moves) {
                HeuristicMove trial = null;
                board.doMove(move);
                
                switch (checkGameOver(currentPlayer, board)) {
                    case WON:
                        board.undoMove(move);
                        return new HeuristicMove(move, getWorstHeuristicEval(board));
                    case LOST:
                        trial = new HeuristicMove(move, getBestHeuristicEval(board));
                        break;
                    case UNSETTLED:
                        trial = getMoveByMaxMinStrategy(getOpponent(currentPlayer),
                                board, depth - 1, OPTIMIZATION_TYPE.MIN);

                }
                
                board.undoMove(move);

                if (trial.getEvaluation() < bestMove.getEvaluation()) {
                    bestMove = new HeuristicMove(move, trial.getEvaluation());
                }
            }
        }

        return bestMove;
    }

    private GAME_RESULT checkGameOver(Color currentPlayer, Board board) {
        if (board.getWinner(currentPlayer) == currentPlayer) {
            return GAME_RESULT.WON;
        }
        Color oponent = getOpponent(currentPlayer);
        if (board.getWinner(oponent) == oponent) {
            return GAME_RESULT.LOST;
        }

        return GAME_RESULT.UNSETTLED;
    }

    private int getPawnsCount(Board board) {
        int boardSize = board.getSize();
        int count = 0;
        for (int x = 0; x < boardSize; x++) {
            for (int y = 0; y < boardSize; y++) {
                Color fieldColor = board.getState(x, y);
                if (fieldColor == getColor()) {
                    count++;
                } else if (fieldColor == getOpponent(getColor())) {
                    count--;
                }
            }
        }
        return count;
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

class HeuristicMove {

    private final Move action;
    private final Integer evaluation;

    public HeuristicMove(Move action, int eval) {
        this.action = action;
        this.evaluation = eval;
    }

    public Move getAction() {
        return action;
    }

    public Integer getEvaluation() {
        return evaluation;
    }
}

enum OPTIMIZATION_TYPE {

    MAX,
    MIN
}

enum GAME_RESULT {

    WON,
    LOST,
    UNSETTLED
}