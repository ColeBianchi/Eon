package com.colebianchi.apps.Eon.adapters;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.colebianchi.apps.Eon.R;
import com.colebianchi.apps.Eon.listeners.RecyclerViewClickListener;

import java.util.List;

public class RVAdapter extends RecyclerView.Adapter<RVAdapter.GameViewHolder>
{
	public static class GameViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
	{
		CardView cv;

		TextView title;
		TextView time;
		ImageView homeLogo;
		RecyclerViewClickListener mListener;

		GameViewHolder(View itemView, RecyclerViewClickListener listener)
		{
			super(itemView);
			cv = itemView.findViewById(R.id.cv);
			title = itemView.findViewById(R.id.title);
			time = itemView.findViewById(R.id.time);
			homeLogo = itemView.findViewById(R.id.homeLogo);
			mListener = listener;
			itemView.setOnClickListener(this);
		}

		@Override
		public void onClick(View view)
		{
			mListener.onClick(view, getAdapterPosition());
		}
	}

	public List<GameCard> gameCards;
	LayoutInflater inflater;
	RecyclerViewClickListener mListener;

	public RVAdapter(List<GameCard> gameCards, LayoutInflater inflater, RecyclerViewClickListener listener)
	{
		this.gameCards = gameCards;
		this.inflater = inflater;
		this.mListener = listener;
	}

	@Override
	public void onAttachedToRecyclerView(RecyclerView recyclerView)
	{
		super.onAttachedToRecyclerView(recyclerView);
	}

	@Override
	public GameViewHolder onCreateViewHolder(ViewGroup viewGroup, int i)
	{
		View v = inflater.inflate(R.layout.game_card, viewGroup, false);
		GameViewHolder pvh = new GameViewHolder(v, mListener);
		return pvh;
	}

	@Override
	public void onBindViewHolder(GameViewHolder personViewHolder, int i)
	{
		personViewHolder.title.setText(gameCards.get(i).title);
		personViewHolder.time.setText(gameCards.get(i).time);
		personViewHolder.homeLogo.setImageResource(gameCards.get(i).id);
	}

	@Override
	public int getItemCount() {
		return gameCards.size();
	}
}
