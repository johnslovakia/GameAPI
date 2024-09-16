package cz.johnslovakia.gameapi.economy;

import cz.johnslovakia.gameapi.users.GamePlayer;

public interface EconomyInterface {

    void deposit(GamePlayer gamePlayer, int amount);
    void withdraw(GamePlayer gamePlayer, int amount);
    void setBalance(GamePlayer gamePlayer, int balance);
    int getBalance(GamePlayer gamePlayer);

}
