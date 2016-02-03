/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package put.ai.games.awesomeplayer;

import java.util.List;
import java.util.Random;
import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;

public class AwesomePlayer extends Player {

    private Random random=new Random(0xdeadbeef);

    @Override
    public String getName() {
        return "Jan Pawlak 117241 Piotr Markowski 117204";
    }

    @Override
    public Move nextMove(Board b) {
        BoardField goalField = getMyGoalField(b);
        BoardField bestPawn = getMyBestPawn(b, goalField);
        int currentDistance = getChebyshevDistance(bestPawn, goalField);
        List<Move> moves = b.getMovesFor(getColor());
        Move bestMove = null;
        int heuristicEvaluation = currentDistance;
        for (Move move : moves) {
            b.doMove(move);
            BoardField newBest = getMyBestPawn(b, goalField);
            int newDist = getChebyshevDistance(newBest, goalField);
            if (newDist < heuristicEvaluation){
                heuristicEvaluation = newDist;
                bestMove = move;
            }
            b.undoMove(move);
        }
        if (bestMove == null){  //hack PM: if situation cannot be improved return radnom move. TODO: will be unhacked with implementing real algorithm
            bestMove = moves.get(random.nextInt(moves.size()));
        }
        return bestMove;
    }
    
    private BoardField getMyBestPawn(Board board, BoardField goalField){
        int boardSize = board.getSize();
        BoardField bestPawn = null;
        for (int x = 0; x < boardSize; x++){
            for (int y = 0; y < boardSize; y++){
                Color fieldColor = board.getState(x, y);
                if (fieldColor == getColor()){
                    BoardField currentField = new BoardField(x, y);
                    if (bestPawn == null 
                            || getChebyshevDistance(currentField, goalField) < getChebyshevDistance(bestPawn, goalField)){
                        bestPawn = currentField;
                    }
                }
            }
        }
        return bestPawn;
    }
    
    private BoardField getMyGoalField(Board board){
        int boardSize = board.getSize();
        BoardField goalField = null;
        if (board.getState(0, 0) == getColor()){    //hack PM: if there is my pawn on (0,0) field and the game isn't finished, my goal is oposite field. TODO: unhack
            goalField = new BoardField(boardSize-1, boardSize-1);
        }
        else{
            goalField = new BoardField(0, 0);
        }
        return goalField;
    }
    
    private int getChebyshevDistance(BoardField fieldA, BoardField fieldB){
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
