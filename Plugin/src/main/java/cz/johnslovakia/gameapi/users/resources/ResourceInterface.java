package cz.johnslovakia.gameapi.users.resources;

import cz.johnslovakia.gameapi.users.GamePlayer;

public interface ResourceInterface {

    void deposit(GamePlayer gamePlayer, int amount);
    void withdraw(GamePlayer gamePlayer, int amount);
    void setBalance(GamePlayer gamePlayer, int balance);
    int getBalance(GamePlayer gamePlayer);

}
