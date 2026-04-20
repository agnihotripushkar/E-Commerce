'use client';

import Link from 'next/link';
import { Product } from '@/lib/types';
import { useCart } from '@/context/CartContext';

const CATEGORY_EMOJI: Record<string, string> = {
  Mats: '🧘',
  Equipment: '💪',
  Apparel: '👕',
  Accessories: '🎒',
  Supplements: '🌿',
};

interface Props {
  product: Product;
}

export default function ProductCard({ product }: Props) {
  const { addItem } = useCart();

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-md transition-shadow group">
      {/* Image placeholder */}
      <Link href={`/products/${product.id}`}>
        <div className="bg-gradient-to-br from-emerald-50 to-teal-100 h-52 flex items-center justify-center text-6xl group-hover:scale-105 transition-transform duration-300">
          {CATEGORY_EMOJI[product.category] ?? '🛍️'}
        </div>
      </Link>

      <div className="p-4">
        <div className="flex items-start justify-between gap-2 mb-1">
          <Link href={`/products/${product.id}`}>
            <h3 className="font-semibold text-gray-900 hover:text-emerald-700 transition-colors line-clamp-2 leading-snug">
              {product.name}
            </h3>
          </Link>
        </div>

        <span className="inline-block text-xs bg-emerald-50 text-emerald-700 font-medium px-2 py-0.5 rounded-full mb-2">
          {product.category}
        </span>

        <p className="text-sm text-gray-500 line-clamp-2 mb-3">{product.description}</p>

        <div className="flex items-center justify-between">
          <span className="text-xl font-bold text-gray-900">${Number(product.price).toFixed(2)}</span>
          <button
            onClick={() => addItem(product)}
            disabled={product.stock === 0}
            className="bg-emerald-600 hover:bg-emerald-700 disabled:bg-gray-200 disabled:text-gray-400 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors"
          >
            {product.stock === 0 ? 'Out of Stock' : 'Add to Cart'}
          </button>
        </div>

        {product.stock > 0 && product.stock <= 5 && (
          <p className="text-xs text-amber-600 mt-2 font-medium">Only {product.stock} left!</p>
        )}
      </div>
    </div>
  );
}
