package com.amoebaman.gamemaster.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

public class UniqueList<E> extends ArrayList<E>{

	private static final long serialVersionUID = -5926738140192336049L;

	@Override
	public boolean add(E element){
		if(element == null || contains(element))
			return false;
		return super.add(element);
	}
	
	@Override
	public boolean addAll(Collection<? extends E> collection){
		boolean success = true;
		for(E element : collection)
			if(!add(element))
				success = false;
		return success;
	}
	
	public E getRandom(){
		return getRandom(new Random());
	}
	
	public E getRandom(Random rand){
		return get((int) (Math.random() * size()));
	}
	
}